package com.tripdesigner.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工作流任务消息。
 *
 * Producer（Controller）发送此消息到 RabbitMQ，Consumer（Worker）消费后执行 LLM 推理。
 *
 * 持久化策略：
 * - deliveryMode=2（持久化），Broker 重启后消息不丢失
 * - 包含 sessionId 和 conversationId，Consumer 消费时无需查询数据库
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskMessage implements Serializable {

    /** 工作流会话 ID（已由 setupSession 创建） */
    private Long sessionId;

    /** 对话 ID */
    private Long conversationId;

    /** 用户 ID（Worker 执行时需要） */
    private Long userId;

    /** 用户邮箱 */
    private String userEmail;

    /** 用户的旅行需求描述 */
    private String userRequest;

    /** 任务创建时间戳（用于监控和超时统计） */
    private long createdAtEpochMillis;
}
