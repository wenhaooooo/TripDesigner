package com.tripdesigner.advisor.api;

import com.tripdesigner.advisor.api.dto.AdvisorRequest;
import com.tripdesigner.advisor.api.vo.AdvisorResponse;
import com.tripdesigner.advisor.application.AdvisorAppService;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 旅行顾问对话 REST API 控制器。
 *
 * 提供两种提问方式：
 * 1. POST /advisor/ask        - 同步提问，返回完整回答
 * 2. POST /advisor/ask/stream - SSE 流式提问，逐块推送回答内容
 */
@Slf4j
@Tag(name = "AI Advisor", description = "AI 旅行顾问对话接口")
@RestController
@RequestMapping("/advisor")
@RequiredArgsConstructor
public class AdvisorController {

    private final AdvisorAppService advisorAppService;

    @Operation(summary = "AI 顾问同步提问", description = "同步调用 AI 旅行顾问，返回完整回答")
    @PostMapping("/ask")
    public Result<AdvisorResponse> ask(@Valid @RequestBody AdvisorRequest req) {
        UserContext ctx = requireAuth();
        AdvisorResponse response = advisorAppService.ask(
                ctx.userId(), req.getQuestion(), req.getConversationId());
        return Result.success(response);
    }

    @Operation(summary = "AI 顾问流式提问",
            description = "SSE 流式推送 AI 旅行顾问的回答内容。"
                    + "前端通过 EventSource 连接，逐块接收内容，最后接收 complete 事件。")
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody AdvisorRequest req) {
        UserContext ctx = requireAuth();
        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时
        AtomicBoolean emitterCompleted = new AtomicBoolean(false);

        emitter.onTimeout(() -> {
            emitterCompleted.set(true);
            emitter.complete();
        });
        emitter.onError(throwable -> emitterCompleted.set(true));

        Thread.startVirtualThread(() -> {
            UserContextHolder.set(ctx);
            try {
                AdvisorResponse response = advisorAppService.askStream(
                        ctx.userId(), req.getQuestion(), req.getConversationId(), content -> {
                            try {
                                if (!emitterCompleted.get()) {
                                    emitter.send(SseEmitter.event().data(content));
                                }
                            } catch (IllegalStateException e) {
                                emitterCompleted.set(true);
                            } catch (Exception e) {
                                emitterCompleted.set(true);
                                log.warn("[AdvisorController] Failed to send SSE chunk: {}", e.getMessage());
                            }
                        });

                if (!emitterCompleted.get()) {
                    emitter.send(SseEmitter.event().name("complete").data(response));
                    emitterCompleted.set(true);
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("[AdvisorController] Streaming advisor failed: {}", e.getMessage(), e);
                try {
                    if (!emitterCompleted.get()) {
                        emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    }
                } catch (Exception ignored) {
                }
                emitterCompleted.set(true);
                emitter.completeWithError(e);
            } finally {
                UserContextHolder.clear();
            }
        });

        return emitter;
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
