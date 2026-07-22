package com.tripdesigner.ai.trip.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.ai.trip.workflow.mapper.WorkflowSessionMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 工作流会话仓储实现。
 *
 * 使用 MyBatis Plus 实现持久化操作。
 * 负责 WorkflowSession 领域实体和 WorkflowSessionPO 数据库对象之间的转换。
 */
@Repository
public class WorkflowSessionRepositoryImpl implements WorkflowSessionRepository {
    private final WorkflowSessionMapper mapper;

    public WorkflowSessionRepositoryImpl(WorkflowSessionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存工作流会话（新增或更新）。
     * 如果 ID 为空则 insert，否则 update。
     *
     * @param session 工作流会话实体
     * @return 持久化后的会话（含生成的 ID）
     */
    @Override
    public WorkflowSession save(WorkflowSession session) {
        WorkflowSessionPO po = toPO(session);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    /**
     * 根据 ID 查找工作流会话。
     *
     * @param id 会话 ID
     * @return Optional 包装的会话实体
     */
    @Override
    public Optional<WorkflowSession> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    /**
     * 更新工作流会话的状态和错误信息。
     * 用于工作流失败时的状态更新。
     *
     * @param id           会话 ID
     * @param status       新状态
     * @param errorMessage 错误信息
     */
    @Override
    public void updateStatus(Long id, WorkflowStatus status, String errorMessage) {
        WorkflowSessionPO po = new WorkflowSessionPO();
        po.setId(id);
        po.setStatus(status.name());
        po.setErrorMessage(errorMessage);
        mapper.updateById(po);
    }

    /**
     * 标记工作流会话为完成状态。
     * 设置 completedAt 和 updatedAt 时间戳。
     *
     * @param id          会话 ID
     * @param completedAt 完成时间
     */
    @Override
    public void complete(Long id, Instant completedAt) {
        WorkflowSessionPO po = new WorkflowSessionPO();
        po.setId(id);
        po.setCompletedAt(completedAt);
        po.setUpdatedAt(Instant.now());
        mapper.updateById(po);
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        mapper.delete(Wrappers.<WorkflowSessionPO>lambdaQuery()
                .eq(WorkflowSessionPO::getConversationId, conversationId));
    }

    /**
     * 将领域实体转换为持久化对象。
     */
    private WorkflowSessionPO toPO(WorkflowSession s) {
        WorkflowSessionPO po = new WorkflowSessionPO();
        po.setId(s.getId());
        po.setConversationId(s.getConversationId());
        po.setUserId(s.getUserId());
        po.setStatus(s.getStatus().name());
        po.setErrorMessage(s.getErrorMessage());
        po.setCompletedAt(s.getCompletedAt());
        return po;
    }

    /**
     * 将持久化对象转换为领域实体。
     * 枚举类型从字符串 valueOf 还原。
     */
    private WorkflowSession fromPO(WorkflowSessionPO po) {
        return WorkflowSession.builder()
                .id(po.getId())
                .conversationId(po.getConversationId())
                .userId(po.getUserId())
                .status(WorkflowStatus.valueOf(po.getStatus()))
                .errorMessage(po.getErrorMessage())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .completedAt(po.getCompletedAt())
                .build();
    }
}
