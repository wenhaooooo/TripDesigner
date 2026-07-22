package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tripdesigner.common.security.UserContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多 Agent 工作流的共享上下文。
 *
 * 工作流中的所有 Agent 共享同一个 AgentContext 实例，实现以下数据传递：
 * 1. 用户原始请求（userRequest）
 * 2. 用户身份信息（userId, userEmail）
 * 3. 偏好摘要和旅行记忆（用于 AI 个性化推荐）
 * 4. 各 Agent 中间输出（通过 sharedData Map）
 * 5. 执行步骤记录（steps 列表，用于追踪和审计）
 *
 * 数据流向：
 * Planner → sharedData["planner_output"] → Transport → sharedData["transport_output"] → ...
 * 每个 Agent 先从 sharedData 读取前置 Agent 的输出，再写入自己的输出。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentContext {

    /** 当前用户的 ID */
    private Long userId;

    /** 当前用户的邮箱 */
    private String userEmail;

    /** 生成的行程 ID（如果有） */
    private Long tripId;

    /** 用户的原始旅行需求描述 */
    private String userRequest;

    /** 各 Agent 的执行步骤记录 */
    private List<AgentStep> steps = new ArrayList<>();

    /**
     * 共享数据存储，key 为 "{agentName}_output"。
     * 例如：planner_output, transport_output, budget_output
     */
    private Map<String, String> sharedData = new HashMap<>();

    /** 用户偏好摘要（已格式化为自然语言，直接注入 Agent 提示词） */
    private String preferenceSummary;

    /** 用户旅行记忆摘要（已格式化为自然语言，直接注入 Agent 提示词） */
    private String tripMemorySummary;

    /** 对话历史摘要（最近 N 条消息，用于让 PlannerAgent 理解连续对话上下文） */
    private String conversationHistory;

    /**
     * RAG 检索的语义相关记忆摘要（方案1）。
     * 通过 RagMemoryService.buildMemoryContext() 生成，
     * 替代全量 preferenceSummary + tripMemorySummary，只注入与当前请求相关的记忆。
     * 若为 null，则 Agent 回退到使用 preferenceSummary + tripMemorySummary。
     */
    private String ragMemoryContext;

    /**
     * RAG 检索的目的地知识摘要（方案2）。
     * 通过 DestinationKnowledgeService.buildKnowledgeContext() 生成，
     * 包含从知识库检索到的景点、美食、交通等实时信息，解决 LLM 幻觉问题。
     */
    private String ragKnowledgeContext;

    /**
     * 用户请求的语言（自动检测）。
     * 所有 Agent 的输出应与此语言保持一致。
     */
    private String userLanguage;

    /**
     * 将 Agent 的输出存入共享数据。
     *
     * @param key   存储键（通常为 "{agentName}_output"）
     * @param value Agent 生成的文本输出
     */
    public void putShared(String key, String value) {
        sharedData.put(key, value);
    }

    /**
     * 从共享数据中读取指定 Agent 的输出。
     *
     * @param key 存储键（通常为 "{agentName}_output"）
     * @return 之前 Agent 生成的文本输出，或 null 如果不存在
     */
    public String getShared(String key) {
        return sharedData.get(key);
    }

    /**
     * 记录一个 Agent 的执行步骤。
     * 包含 Agent 名称、输出内容、成功状态和执行次数。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AgentStep {
        /** 执行该步骤的 Agent 名称 */
        private String agentName;

        /** Agent 输出的文本内容 */
        private String output;

        /** 是否执行成功 */
        private boolean success;

        /** 执行次数（第几次尝试） */
        private Integer iteration;
    }

    /**
     * 添加一个 Agent 执行步骤记录。
     *
     * @param agentName Agent 名称
     * @param output    Agent 输出
     * @param success   是否成功
     * @param iteration 执行次数
     */
    public void addStep(String agentName, String output, boolean success, Integer iteration) {
        steps.add(new AgentStep(agentName, output, success, iteration));
    }

    /**
     * 按 Agent 名称查询执行步骤。
     *
     * @param agentName Agent 名称
     * @return 该 Agent 的所有执行步骤（可能有多次重试）
     */
    public List<AgentStep> getStepByAgent(String agentName) {
        return steps.stream()
                .filter(s -> s.getAgentName().equals(agentName))
                .toList();
    }
}
