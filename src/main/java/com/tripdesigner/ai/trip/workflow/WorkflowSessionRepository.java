package com.tripdesigner.ai.trip.workflow;

import java.time.Instant;
import java.util.Optional;

/**
 * 工作流会话仓储接口。
 *
 * 定义工作流会话的持久化操作：
 * - save: 创建或更新会话
 * - findById: 根据 ID 查找
 * - updateStatus: 更新状态（用于标记失败）
 * - complete: 标记为完成（设置 completedAt）
 * - deleteByConversationId: 根据对话ID删除关联的工作流会话
 */
public interface WorkflowSessionRepository {
    WorkflowSession save(WorkflowSession session);
    Optional<WorkflowSession> findById(Long id);
    void updateStatus(Long id, WorkflowStatus status, String errorMessage);
    void complete(Long id, Instant completedAt);
    void deleteByConversationId(Long conversationId);
}
