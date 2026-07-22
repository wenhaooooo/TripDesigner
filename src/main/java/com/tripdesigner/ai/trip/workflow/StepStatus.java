package com.tripdesigner.ai.trip.workflow;

/**
 * 工作流步骤状态枚举。
 *
 * 状态流转：
 * PENDING → RUNNING → COMPLETED（执行成功）
 *                   → FAILED（执行失败）
 *                   → RETRYING（正在重试）
 *                   → SKIPPED（跳过执行）
 */
public enum StepStatus {

    /** 初始状态：等待执行 */
    PENDING,

    /** 执行中：Agent 正在调用 LLM */
    RUNNING,

    /** 已完成：Agent 成功执行并返回结果 */
    COMPLETED,

    /** 已失败：Agent 执行出错 */
    FAILED,

    /** 重试中：上一次执行失败，正在重试 */
    RETRYING,

    /** 已跳过：因前置条件不满足而跳过 */
    SKIPPED
}
