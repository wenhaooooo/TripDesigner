package com.tripdesigner.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 工作流消息生产者。
 *
 * 由 Controller 调用，将 LLM 推理任务发送到 RabbitMQ 队列，立即返回。
 * Worker（Consumer）异步消费并执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送工作流任务到队列。
     *
     * 消息持久化策略：
     * - 队列 durable=true（见 RabbitMQConfig）
     * - 消息默认持久化（Spring AMQP 默认 MessageDeliveryMode.PERSISTENT）
     *
     * @param task 任务消息
     */
    public void sendWorkflowTask(WorkflowTaskMessage task) {
        log.info("[MQ Producer] Sending workflow task: sessionId={}, user={}",
                task.getSessionId(), task.getUserId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.WORKFLOW_EXCHANGE,
                RabbitMQConfig.WORKFLOW_ROUTING_KEY,
                task);
    }

    /**
     * 发送工作流结果通知。
     *
     * 路由键使用 workflow.result.{sessionId}，前端按 sessionId 订阅 WebSocket。
     *
     * @param result 结果消息
     */
    public void sendWorkflowResult(WorkflowResultMessage result) {
        String routingKey = RabbitMQConfig.WORKFLOW_RESULT_ROUTING_KEY + "." + result.getSessionId();
        log.info("[MQ Producer] Sending workflow result: sessionId={}, status={}",
                result.getSessionId(), result.getStatus());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.WORKFLOW_RESULT_EXCHANGE,
                routingKey,
                result);
    }
}
