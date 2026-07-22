package com.tripdesigner.common.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑配置。
 *
 * 架构：
 *   Producer → workflow.exchange (direct) → workflow.queue → Consumer
 *   Consumer 完成后 → workflow.result.exchange (topic) → workflow.result.queue → WebSocket 推送
 *
 * 容错：
 *   - 两个主队列均配置 DLX，消费失败的消息转入 DLQ 便于排查
 *   - 配合 spring.rabbitmq.listener.simple.default-requeue-rejected=false
 *     使被拒绝的消息进入 DLQ 而非反复重入主队列
 *
 * 队列持久化：durable=true，Broker 重启后队列和消息不丢失
 */
@Configuration
public class RabbitMQConfig {

    // ========== 任务队列（Producer → Consumer） ==========
    public static final String WORKFLOW_EXCHANGE = "workflow.exchange";
    public static final String WORKFLOW_QUEUE = "workflow.queue";
    public static final String WORKFLOW_ROUTING_KEY = "workflow.task";

    // ========== 任务 DLQ ==========
    public static final String WORKFLOW_DLX = "workflow.dlx";
    public static final String WORKFLOW_DLQ = "workflow.dlq";
    public static final String WORKFLOW_DLQ_ROUTING_KEY = "workflow.task.dead";

    // ========== 结果通知（Consumer → WebSocket） ==========
    public static final String WORKFLOW_RESULT_EXCHANGE = "workflow.result.exchange";
    public static final String WORKFLOW_RESULT_QUEUE = "workflow.result.queue";
    public static final String WORKFLOW_RESULT_ROUTING_KEY = "workflow.result";

    // ========== 结果 DLQ ==========
    public static final String WORKFLOW_RESULT_DLX = "workflow.result.dlx";
    public static final String WORKFLOW_RESULT_DLQ = "workflow.result.dlq";
    public static final String WORKFLOW_RESULT_DLQ_ROUTING_KEY = "workflow.result.dead";

    /**
     * 任务队列：配置 DLX，消费失败的消息转入 workflow.dlq
     */
    @Bean
    public Queue workflowQueue() {
        return QueueBuilder.durable(WORKFLOW_QUEUE)
                .deadLetterExchange(WORKFLOW_DLX)
                .deadLetterRoutingKey(WORKFLOW_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange workflowExchange() {
        return new DirectExchange(WORKFLOW_EXCHANGE, true, false);
    }

    @Bean
    public Binding workflowBinding() {
        return BindingBuilder.bind(workflowQueue())
                .to(workflowExchange())
                .with(WORKFLOW_ROUTING_KEY);
    }

    // ========== 任务 DLQ 拓扑 ==========
    @Bean
    public DirectExchange workflowDlx() {
        return new DirectExchange(WORKFLOW_DLX, true, false);
    }

    @Bean
    public Queue workflowDlq() {
        return QueueBuilder.durable(WORKFLOW_DLQ).build();
    }

    @Bean
    public Binding workflowDlqBinding() {
        return BindingBuilder.bind(workflowDlq())
                .to(workflowDlx())
                .with(WORKFLOW_DLQ_ROUTING_KEY);
    }

    // ========== 结果通知拓扑 ==========
    @Bean
    public TopicExchange workflowResultExchange() {
        return new TopicExchange(WORKFLOW_RESULT_EXCHANGE, true, false);
    }

    /**
     * 结果队列：配置 DLX，消费失败的消息转入 workflow.result.dlq
     */
    @Bean
    public Queue workflowResultQueue() {
        return QueueBuilder.durable(WORKFLOW_RESULT_QUEUE)
                .deadLetterExchange(WORKFLOW_RESULT_DLX)
                .deadLetterRoutingKey(WORKFLOW_RESULT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding workflowResultBinding() {
        return BindingBuilder.bind(workflowResultQueue())
                .to(workflowResultExchange())
                .with(WORKFLOW_RESULT_ROUTING_KEY + ".#");
    }

    // ========== 结果 DLQ 拓扑 ==========
    @Bean
    public DirectExchange workflowResultDlx() {
        return new DirectExchange(WORKFLOW_RESULT_DLX, true, false);
    }

    @Bean
    public Queue workflowResultDlq() {
        return QueueBuilder.durable(WORKFLOW_RESULT_DLQ).build();
    }

    @Bean
    public Binding workflowResultDlqBinding() {
        return BindingBuilder.bind(workflowResultDlq())
                .to(workflowResultDlx())
                .with(WORKFLOW_RESULT_DLQ_ROUTING_KEY);
    }

    // ========== 公共组件 ==========
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
