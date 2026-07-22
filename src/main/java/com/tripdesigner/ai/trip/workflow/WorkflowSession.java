package com.tripdesigner.ai.trip.workflow;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 工作流会话实体 —— 代表一次多 Agent 工作流的执行过程。
 *
 * 跟踪工作流的整个生命周期：
 * PENDING → RUNNING → COMPLETED（正常结束）
 *                    → FAILED（异常终止）
 *
 * 每个工作流会话关联一个用户和一个对话，
 * 包含多个 WorkflowStep（每个 Agent 的执行记录）。
 */
@Getter
@Builder
public class WorkflowSession {
    private Long id;
    private Long conversationId;
    private Long userId;
    private WorkflowStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    /**
     * 创建工作流会话实例（初始状态为 PENDING）。
     *
     * @param conversationId 关联的对话 ID
     * @param userId         用户 ID
     * @return 新的 WorkflowSession
     */
    public static WorkflowSession create(Long conversationId, Long userId) {
        return WorkflowSession.builder()
                .conversationId(conversationId)
                .userId(userId)
                .status(WorkflowStatus.PENDING)
                .build();
    }

    /**
     * 生成一个新的 WorkflowSession 实例，仅变更状态字段。
     * 遵循不可变对象模式，所有字段拷贝自当前实例。
     *
     * @param status 新的状态
     * @return 更新了状态的新实例
     */
    public WorkflowSession withStatus(WorkflowStatus status) {
        return WorkflowSession.builder()
                .id(id).conversationId(conversationId).userId(userId)
                .status(status).errorMessage(errorMessage)
                .createdAt(createdAt).updatedAt(updatedAt).completedAt(completedAt)
                .build();
    }

    /**
     * 生成一个新的 WorkflowSession 实例，设置完成状态和完成时间。
     *
     * @param status 完成状态（通常为 COMPLETED）
     * @return 更新了完成信息的新实例
     */
    public WorkflowSession withCompleted(WorkflowStatus status) {
        return WorkflowSession.builder()
                .id(id).conversationId(conversationId).userId(userId)
                .status(status).errorMessage(errorMessage)
                .createdAt(createdAt).updatedAt(updatedAt).completedAt(Instant.now())
                .build();
    }
}
