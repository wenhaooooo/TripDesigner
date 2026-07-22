package com.tripdesigner.ai.trip;

import com.tripdesigner.ai.trip.agent.WorkflowAsyncRunner;
import com.tripdesigner.ai.trip.agent.WorkflowEngine;
import com.tripdesigner.ai.trip.dto.GenerateRequest;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.mq.WorkflowMessageProducer;
import com.tripdesigner.common.mq.WorkflowTaskMessage;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.application.TripAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Controller for multi-agent workflow operations.
 *
 * 提供三种生成行程方式：
 * 1. POST /generate          - 同步执行（阻塞，5+分钟）
 * 2. POST /generate/async    - JVM 内 @Async 异步（@Deprecated，单机不持久化）
 * 3. POST /generate/mq       - RabbitMQ 异步（推荐，持久化 + 削峰）
 */
@Slf4j
@Tag(name = "AI Workflow", description = "多 Agent 工作流接口（生成行程、查询进度、取消）")
@RestController
@RequestMapping("/ai/workflow")
@RequiredArgsConstructor
public class MultiAgentController {

    private final WorkflowEngine workflowEngine;
    private final WorkflowAsyncRunner workflowAsyncRunner;
    private final WorkflowMessageProducer messageProducer;
    private final TripAppService tripAppService;

    @Operation(summary = "生成行程（同步）", description = "同步执行完整的多 Agent 工作流，可能阻塞 5+ 分钟。推荐使用 POST /generate/mq")
    @PostMapping("/generate")
    public Result<WorkflowEngine.WorkflowResult> generate(
            @RequestBody @Valid GenerateRequest request) {
        UserContext ctx = requireAuth();
        return Result.success(workflowEngine.execute(
                ctx.userId(), ctx.email(), request.getConversationId(), request.getPrompt()));
    }

    @Operation(summary = "生成行程（@Async 异步）", description = "JVM 内异步执行，服务器重启会丢失任务。推荐使用 POST /generate/mq")
    @PostMapping("/generate/async")
    public Result<Map<String, Object>> generateAsync(
            @RequestBody @Valid GenerateRequest request) {
        UserContext ctx = requireAuth();
        WorkflowEngine.SetupResult setup = workflowEngine.setupSessionPublic(
                ctx.userId(), request.getConversationId(), request.getPrompt());
        workflowAsyncRunner.executeAsync(
                ctx.userId(), ctx.email(), setup.conversationId(), setup.sessionId(), request.getPrompt());
        return buildAsyncResponse(setup);
    }

    @Operation(summary = "生成行程（RabbitMQ 异步，推荐）",
            description = "立即返回工作流会话 ID，任务持久化到 RabbitMQ，Worker 异步消费执行。"
                    + "前端可通过 GET /{sessionId} 轮询或订阅 WebSocket /ws/workflow/{sessionId} 实时接收结果")
    @PostMapping("/generate/mq")
    public Result<Map<String, Object>> generateMq(
            @RequestBody @Valid GenerateRequest request) {
        UserContext ctx = requireAuth();
        // 同步创建会话，立即返回 sessionId
        WorkflowEngine.SetupResult setup = workflowEngine.setupSessionPublic(
                ctx.userId(), request.getConversationId(), request.getPrompt());
        // 发送任务到 MQ，Worker 异步消费
        WorkflowTaskMessage task = WorkflowTaskMessage.builder()
                .sessionId(setup.sessionId())
                .conversationId(setup.conversationId())
                .userId(ctx.userId())
                .userEmail(ctx.email())
                .userRequest(request.getPrompt())
                .createdAtEpochMillis(System.currentTimeMillis())
                .build();
        messageProducer.sendWorkflowTask(task);
        return buildAsyncResponse(setup);
    }

    @Operation(summary = "生成行程（SSE 流式，实时推送）",
            description = "SSE 流式执行工作流，实时推送每个 Agent 的输出内容。"
                    + "前端通过 EventSource 连接，接收 agent_start/agent_content/agent_end/summary/complete 等事件。"
                    + "总耗时取决于 LLM 响应速度，通常 2-5 分钟。")
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@RequestBody @Valid GenerateRequest request) {
        UserContext ctx = requireAuth();
        SseEmitter emitter = new SseEmitter(600000L);
        java.util.concurrent.atomic.AtomicBoolean emitterCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);

        emitter.onTimeout(() -> {
            emitterCompleted.set(true);
            emitter.complete();
        });
        emitter.onError(throwable -> {
            emitterCompleted.set(true);
        });

        Thread.startVirtualThread(() -> {
            UserContextHolder.set(ctx);
            try {
                workflowEngine.streamExecute(
                        ctx.userId(), ctx.email(), request.getConversationId(),
                        request.getPrompt(), sseMessage -> {
                            try {
                                if (!emitterCompleted.get()) {
                                    emitter.send(SseEmitter.event()
                                            .name(sseMessage.getType())
                                            .data(sseMessage));
                                }
                            } catch (IllegalStateException e) {
                                emitterCompleted.set(true);
                            } catch (Exception e) {
                                log.warn("[MultiAgentController] Failed to send SSE message: {}", e.getMessage());
                            }
                        });
                emitterCompleted.set(true);
                emitter.complete();
            } catch (Exception e) {
                log.error("[MultiAgentController] Streaming workflow failed: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(WorkflowEngine.SseMessage.error(null, e.getMessage())));
                } catch (Exception ignored) {
                }
                emitterCompleted.set(true);
                emitter.complete();
            } finally {
                UserContextHolder.clear();
            }
        });

        return emitter;
    }

    @Operation(summary = "查询工作流详情", description = "获取指定工作流会话的执行状态和各 Agent 步骤")
    @GetMapping("/{sessionId}")
    public Result<WorkflowEngine.WorkflowDetails> getDetails(
            @Parameter(description = "工作流会话 ID") @PathVariable Long sessionId) {
        requireAuth();
        WorkflowEngine.WorkflowDetails details = workflowEngine.getWorkflowDetails(sessionId);
        return Result.success(details);
    }

    @Operation(summary = "取消工作流", description = "协作式取消，在下一个 Agent 执行前生效")
    @PostMapping("/{sessionId}/cancel")
    public Result<Void> cancel(
            @Parameter(description = "工作流会话 ID") @PathVariable Long sessionId) {
        requireAuth();
        workflowEngine.cancel(sessionId);
        return Result.success(null);
    }

    @Operation(summary = "查询行程详情", description = "根据行程 ID 获取行程完整信息")
    @GetMapping("/trip/{tripId}")
    public Result<TripDetailVo> getTrip(
            @Parameter(description = "行程 ID") @PathVariable Long tripId) {
        UserContext ctx = requireAuth();
        TripDetailVo trip = tripAppService.getDetailForUser(tripId, ctx.userId());
        return Result.success(trip);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private Result<Map<String, Object>> buildAsyncResponse(WorkflowEngine.SetupResult setup) {
        return Result.success(Map.of(
                "sessionId", setup.sessionId(),
                "conversationId", setup.conversationId(),
                "status", "RUNNING",
                "progressUrl", "/ai/workflow/" + setup.sessionId(),
                "websocketUrl", "/ws/workflow/" + setup.sessionId()));
    }
}
