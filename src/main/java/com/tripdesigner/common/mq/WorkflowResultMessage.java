package com.tripdesigner.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工作流结果通知消息。
 *
 * Consumer（Worker）执行完成后发送此消息，由 WebSocket 端点订阅并推送给前端。
 *
 * 路由键：workflow.result.{sessionId}，前端按 sessionId 订阅
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResultMessage implements Serializable {

    /** 工作流会话 ID */
    private Long sessionId;

    /** 对话 ID */
    private Long conversationId;

    /** 用户 ID */
    private Long userId;

    /** 执行状态：COMPLETED / FAILED / CANCELLED */
    private String status;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 生成的行程 ID（成功时返回） */
    private Long tripId;

    /** 完成时间戳 */
    private long completedAtEpochMillis;
}
