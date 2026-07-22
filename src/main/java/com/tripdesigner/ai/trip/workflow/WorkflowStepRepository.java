package com.tripdesigner.ai.trip.workflow;

import java.util.List;
import java.util.Optional;

/**
 * 工作流步骤仓储接口。
 *
 * 定义工作流步骤的持久化操作：
 * - save: 创建或更新步骤记录
 * - findById: 根据 ID 查找
 * - findBySessionId: 查找某会话的所有步骤（按创建时间升序）
 *
 * 实现在 infrastructure 层（WorkflowStepRepositoryImpl），
 * 使用 MyBatis Plus 进行数据库操作。
 */
public interface WorkflowStepRepository {
    WorkflowStep save(WorkflowStep step);
    Optional<WorkflowStep> findById(Long id);
    List<WorkflowStep> findBySessionId(Long sessionId);
}
