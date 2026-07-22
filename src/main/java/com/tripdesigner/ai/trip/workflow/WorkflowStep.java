package com.tripdesigner.ai.trip.workflow;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 工作流步骤实体 —— 代表工作流中一个 Agent 的单次执行记录。
 *
 * 状态流转：PENDING → RUNNING → COMPLETED（成功执行）
 *                                → FAILED（执行出错）
 *
 * 每次重试都会创建新的 WorkflowStep 记录，
 * iteration 字段记录这是第几次尝试。
 */
@Getter
@Builder
public class WorkflowStep {
    private Long id;
    private Long sessionId;
    private String agentName;
    private StepStatus status;
    private String inputContext;
    private String outputResult;
    private String errorMessage;
    private Integer iteration;
    private Instant startedAt;
    private Instant completedAt;

    /**
     * 创建工作流步骤（初始状态 PENDING，iteration=1）。
     *
     * @param sessionId 所属工作流会话 ID
     * @param agentName Agent 名称
     * @return 新的 WorkflowStep 实例
     */
    public static WorkflowStep create(Long sessionId, String agentName) {
        return WorkflowStep.builder()
                .sessionId(sessionId)
                .agentName(agentName)
                .status(StepStatus.PENDING)
                .iteration(1)
                .build();
    }

    /**
     * 生成新的 WorkflowStep 实例，标记为 RUNNING 并记录开始时间。
     *
     * @param startedAt 开始执行的时间
     * @return 更新后的实例
     */
    public WorkflowStep withRunning(Instant startedAt) {
        return WorkflowStep.builder()
                .id(id).sessionId(sessionId).agentName(agentName)
                .status(StepStatus.RUNNING)
                .inputContext(inputContext).outputResult(outputResult)
                .errorMessage(errorMessage).iteration(iteration)
                .startedAt(startedAt).completedAt(completedAt)
                .build();
    }

    /**
     * 生成新的 WorkflowStep 实例，标记为 COMPLETED 并记录输出。
     *
     * @param output     Agent 生成的输出文本
     * @param completedAt 完成时间
     * @return 更新后的实例
     */
    public WorkflowStep withCompleted(String output, Instant completedAt) {
        return WorkflowStep.builder()
                .id(id).sessionId(sessionId).agentName(agentName)
                .status(StepStatus.COMPLETED)
                .inputContext(inputContext).outputResult(output)
                .errorMessage(null).iteration(iteration)
                .startedAt(startedAt).completedAt(completedAt)
                .build();
    }

    /**
     * 生成新的 WorkflowStep 实例，标记为 FAILED 并记录错误信息。
     *
     * @param error       错误描述
     * @param completedAt 失败时间
     * @return 更新后的实例
     */
    public WorkflowStep withFailed(String error, Instant completedAt) {
        return WorkflowStep.builder()
                .id(id).sessionId(sessionId).agentName(agentName)
                .status(StepStatus.FAILED)
                .inputContext(inputContext).outputResult(outputResult)
                .errorMessage(error).iteration(iteration)
                .startedAt(startedAt).completedAt(completedAt)
                .build();
    }
}
