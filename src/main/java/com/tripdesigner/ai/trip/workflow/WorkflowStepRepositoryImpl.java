package com.tripdesigner.ai.trip.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.ai.trip.workflow.mapper.WorkflowStepMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流步骤仓储实现。
 *
 * 使用 MyBatis Plus 实现持久化操作。
 * 负责 WorkflowStep 领域实体和 WorkflowStepPO 数据库对象之间的转换。
 * 提供按会话 ID 查询步骤列表功能（按创建时间升序排列）。
 */
@Repository
public class WorkflowStepRepositoryImpl implements WorkflowStepRepository {
    private final WorkflowStepMapper mapper;

    public WorkflowStepRepositoryImpl(WorkflowStepMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存工作流步骤（新增或更新）。
     *
     * @param step 工作流步骤实体
     * @return 持久化后的步骤（含生成的 ID）
     */
    @Override
    public WorkflowStep save(WorkflowStep step) {
        WorkflowStepPO po = toPO(step);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    /**
     * 根据 ID 查找工作流步骤。
     *
     * @param id 步骤 ID
     * @return Optional 包装的步骤实体
     */
    @Override
    public Optional<WorkflowStep> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    /**
     * 查找指定工作流会话的所有步骤。
     * 按创建时间升序排列，便于按执行顺序展示。
     *
     * @param sessionId 工作流会话 ID
     * @return 步骤列表
     */
    @Override
    public List<WorkflowStep> findBySessionId(Long sessionId) {
        return mapper.selectList(
                Wrappers.<WorkflowStepPO>lambdaQuery()
                        .eq(WorkflowStepPO::getSessionId, sessionId)
                        .orderByAsc(WorkflowStepPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    /**
     * 将领域实体转换为持久化对象。
     */
    private WorkflowStepPO toPO(WorkflowStep s) {
        WorkflowStepPO po = new WorkflowStepPO();
        po.setId(s.getId());
        po.setSessionId(s.getSessionId());
        po.setAgentName(s.getAgentName());
        po.setStatus(s.getStatus().name());
        po.setInputContext(s.getInputContext());
        po.setOutputResult(s.getOutputResult());
        po.setErrorMessage(s.getErrorMessage());
        po.setIteration(s.getIteration());
        po.setStartedAt(s.getStartedAt());
        po.setCompletedAt(s.getCompletedAt());
        return po;
    }

    /**
     * 将持久化对象转换为领域实体。
     * 枚举类型从字符串 valueOf 还原。
     */
    private WorkflowStep fromPO(WorkflowStepPO po) {
        return WorkflowStep.builder()
                .id(po.getId())
                .sessionId(po.getSessionId())
                .agentName(po.getAgentName())
                .status(StepStatus.valueOf(po.getStatus()))
                .inputContext(po.getInputContext())
                .outputResult(po.getOutputResult())
                .errorMessage(po.getErrorMessage())
                .iteration(po.getIteration())
                .startedAt(po.getStartedAt())
                .completedAt(po.getCompletedAt())
                .build();
    }
}
