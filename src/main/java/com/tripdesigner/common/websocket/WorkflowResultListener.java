package com.tripdesigner.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.mq.RabbitMQConfig;
import com.tripdesigner.common.mq.WorkflowResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 工作流结果监听器。
 *
 * 从 RabbitMQ 消费工作流结果，通过 WebSocket 推送给已连接的前端客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowResultListener {

    private final WorkflowWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.WORKFLOW_RESULT_QUEUE)
    public void onWorkflowResult(WorkflowResultMessage result) {
        log.info("[WS Listener] Received workflow result: sessionId={}, status={}",
                result.getSessionId(), result.getStatus());
        try {
            String json = objectMapper.writeValueAsString(result);
            handler.sendResult(String.valueOf(result.getSessionId()), json);
        } catch (Exception e) {
            log.warn("[WS Listener] Failed to serialize result for sessionId={}", result.getSessionId(), e);
        }
    }
}
