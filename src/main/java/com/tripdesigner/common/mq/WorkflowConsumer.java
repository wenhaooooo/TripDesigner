package com.tripdesigner.common.mq;

import com.tripdesigner.ai.trip.agent.WorkflowEngine;
import com.tripdesigner.ai.trip.workflow.WorkflowStatus;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 工作流任务消费者（Worker）。
 *
 * 从 workflow.queue 消费任务，调用 WorkflowEngine 执行 LLM 推理。
 * 完成后发送结果通知到 workflow.result.exchange，由 WebSocket 端点推送给前端。
 *
 * 失败处理策略：
 * - LLM 调用失败由 Agent 内部重试（默认 3 次，线性退避）
 * - 工作流异常已由 executeCore 捕获并标记 session 为 FAILED
 * - MQ 层面不重新入队，避免重复 LLM 调用浪费成本
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowConsumer {

    private final WorkflowEngine workflowEngine;
    private final WorkflowMessageProducer messageProducer;

    @RabbitListener(queues = RabbitMQConfig.WORKFLOW_QUEUE, concurrency = "1-4")
    public void handleWorkflowTask(WorkflowTaskMessage task) {
        long startTime = System.currentTimeMillis();
        log.info("[MQ Consumer] Received workflow task: sessionId={}, user={}, queueWaitMs={}",
                task.getSessionId(), task.getUserId(),
                startTime - task.getCreatedAtEpochMillis());

        try {
            UserContextHolder.set(new UserContext(task.getUserId(), task.getUserEmail()));
            WorkflowEngine.WorkflowResult result = workflowEngine.executeWithExistingSession(
                    task.getUserId(),
                    task.getUserEmail(),
                    task.getConversationId(),
                    task.getSessionId(),
                    task.getUserRequest());

            Long tripId = result.getTrip() != null ? result.getTrip().getId() : null;
            sendResult(task, WorkflowStatus.COMPLETED.name(), null, tripId);
            log.info("[MQ Consumer] Workflow completed: sessionId={}, durationMs={}",
                    task.getSessionId(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[MQ Consumer] Workflow failed: sessionId={}, durationMs={}",
                    task.getSessionId(), System.currentTimeMillis() - startTime, e);
            // executeCore 已将 session 标记为 FAILED，这里只发送通知
            sendResult(task, WorkflowStatus.FAILED.name(), e.getMessage(), null);
            // 不抛出异常，避免 MQ 重新入队（default-requeue-rejected=false 也会丢弃）
        } finally {
            UserContextHolder.clear();
        }
    }

    private void sendResult(WorkflowTaskMessage task, String status, String errorMessage, Long tripId) {
        WorkflowResultMessage result = WorkflowResultMessage.builder()
                .sessionId(task.getSessionId())
                .conversationId(task.getConversationId())
                .userId(task.getUserId())
                .status(status)
                .errorMessage(errorMessage)
                .tripId(tripId)
                .completedAtEpochMillis(System.currentTimeMillis())
                .build();
        messageProducer.sendWorkflowResult(result);
    }
}
