package com.tripdesigner.ai.trip.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.ai.trip.workflow.WorkflowStepPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowStepMapper extends BaseMapper<WorkflowStepPO> {
}