package com.tripdesigner.ai.trip.workflow;

/**
 * 工作流会话状态枚举。
 *
 * 状态流转：
 * PENDING → RUNNING → COMPLETED（正常完成）
 *                   → FAILED（异常终止）
 *                   → CANCELLED（用户取消）
 */
public enum WorkflowStatus {

    /** 初始状态：工作流已创建，等待执行 */
    PENDING,

    /** 执行中：至少有一个 Agent 正在运行 */
    RUNNING,

    /** 已完成：所有 Agent 执行完毕，结果已保存 */
    COMPLETED,

    /** 已失败：工作流执行过程中出现不可恢复错误 */
    FAILED,

    /** 已取消：用户主动取消工作流 */
    CANCELLED
}
