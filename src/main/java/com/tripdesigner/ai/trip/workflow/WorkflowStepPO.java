package com.tripdesigner.ai.trip.workflow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 工作流步骤数据库持久化对象（PO）。
 *
 * 映射至 workflow_steps 表。
 * 记录每个 Agent 单次执行的全过程：
 * - inputContext: 执行时的上下文（可用 JSON 存储）
 * - outputResult: Agent 生成的文本输出
 * - iteration: 第几次尝试（重试次数记录）
 * - startedAt/completedAt: 执行时间范围
 */
@Data
@TableName("workflow_steps")
public class WorkflowStepPO {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属工作流会话 ID */
    private Long sessionId;

    /** Agent 名称 */
    private String agentName;

    /** 步骤状态（StepStatus 名称） */
    private String status;

    /** 输入上下文（JSON 格式） */
    private String inputContext;

    /** 输出结果（Agent 生成的文本） */
    private String outputResult;

    /** 错误信息（执行失败时填充） */
    private String errorMessage;

    /** 执行次数（初次=1，重试递增） */
    private Integer iteration;

    /** 开始执行时间 */
    private Instant startedAt;

    /** 完成/失败时间 */
    private Instant completedAt;

    /** 创建时间（MyBatis Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /** 更新时间（MyBatis Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
