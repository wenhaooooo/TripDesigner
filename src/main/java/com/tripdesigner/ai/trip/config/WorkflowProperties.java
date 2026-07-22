package com.tripdesigner.ai.trip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工作流配置属性。
 *
 * 在 application.yml 中通过 `workflow.*` 配置：
 *
 * workflow:
 *   agent-order:
 *     - planner
 *     - transport
 *     - dining
 *     - sightseeing
 *     - accommodation
 *     - budget
 *     - activity
 *     - reflection
 *   max-consecutive-failures: 2
 *   conversation-history-limit: 6
 *   agent-timeout-seconds: 300
 *   skip-on-failure: transport,dining,sightseeing,accommodation,budget
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    /**
     * Agent 执行顺序（按列表顺序依次执行）。
     * 后续 Agent 可依赖前置 Agent 的输出。
     */
    private List<String> agentOrder = List.of(
            "planner", "transport", "dining", "sightseeing",
            "accommodation", "budget", "activity", "reflection"
    );

    /** 连续失败次数达到此阈值时中断工作流 */
    private int maxConsecutiveFailures = 2;

    /** 对话历史注入条数（避免 token 爆炸） */
    private int conversationHistoryLimit = 6;

    /**
     * 单个 Agent 执行的超时时间（秒）。
     * 0 表示不超时（向后兼容，测试默认值）。
     * 生产环境建议 300 秒（5分钟），避免单个 Agent 阻塞整个工作流。
     */
    private long agentTimeoutSeconds = 0;

    /**
     * 失败时跳过的 Agent 列表（逗号分隔）。
     * 这些 Agent 的失败不会中断工作流，也不计入连续失败计数。
     * 适用于非关键 Agent（如交通、餐饮、住宿建议等）。
     */
    private String skipOnFailure = "transport,dining,sightseeing,accommodation,budget";

    /**
     * 是否启用并行执行。
     * 启用后，Transport、Dining、Sightseeing、Accommodation、Budget 会并行执行，
     * 大幅缩短工作流总耗时（从串行的 5*N 秒降至约 N 秒）。
     */
    private boolean parallelExecutionEnabled = true;

    /**
     * 检查指定 Agent 是否在失败时跳过。
     *
     * @param agentName Agent 名称
     * @return true 表示该 Agent 失败时跳过
     */
    public boolean shouldSkipOnFailure(String agentName) {
        if (skipOnFailure == null || skipOnFailure.isEmpty()) {
            return false;
        }
        for (String name : skipOnFailure.split(",")) {
            if (name.trim().equals(agentName)) {
                return true;
            }
        }
        return false;
    }
}
