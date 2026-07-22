package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Budget Agent（预算 Agent）。
 *
 * 工作流中的第六个 Agent，负责预算分配和优化。
 * 在交通、餐饮、景点、住宿等 Agent 给出推荐后，
 * 此 Agent 将总预算合理分配到各开销类别。
 *
 * 预算分类：
 * - 交通（城际 + 本地）
 * - 住宿
 * - 餐饮
 * - 景点门票/活动
 * - 购物/纪念品
 * - 应急备用金
 * - 其他杂项
 *
 * 个性化：根据用户偏好调整各类别比例。
 * 例如"美食爱好者" → 餐饮预算占比提高。
 */
@Slf4j
@Component
public class BudgetAgent extends AbstractAgent {

    /**
     * 系统提示词：定义预算专家的角色。
     * 要求进行详细的预算分解，包含百分比分配、费用范围和优化建议。
     */
    private static final String SYSTEM_PROMPT = """
            You are a budget specialist for travel planning.
            Your job is to analyze and optimize the trip budget across different categories.

            Given the trip details (total budget, destinations, activities, preferences),
            you should provide:
            1. Detailed budget breakdown by category:
               - Transportation (inter-city + local)
               - Accommodation
               - Food & dining
               - Activities & attractions
               - Shopping & souvenirs
               - Emergency fund
               - Miscellaneous
            2. Percentage allocation for each category
            3. Cost estimates with ranges
            4. Budget optimization tips

            Format your recommendations clearly with:
            - Category name
            - Recommended budget amount/range
            - Percentage of total budget
            - Rationale

            Ensure the budget is realistic and leaves room for flexibility.
            Prioritize experiences over material goods.
            """;

    public BudgetAgent(ChatClient chatClient, ObjectMapper objectMapper) {
        super(chatClient, objectMapper);
    }

    @Override
    public String getName() {
        return "budget";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 执行 Budget Agent：
     * 综合 Planner 的行程计划和其他 Agent 的推荐，
     * 生成详细的预算分解方案。
     * 根据用户偏好调整各类别的预算分配比例。
     *
     * @param context 共享上下文
     * @return 预算分解方案文本
     */
    @Override
    public String execute(AgentContext context) {
        String plan = context.getShared("planner_output");
        String tripDetail = context.getShared("trip_detail");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";
        String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";

        String prompt = """
                Here is the trip plan from the Planner Agent:
                %s

                Current trip details:
                %s

                User Preferences (personalize budget allocations):
                %s

                User's original request: %s

                Based on this information, provide a detailed budget breakdown
                with recommended allocations for each category.
                Adjust allocations based on user preferences (e.g., foodie = higher dining budget).
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);

        String result = chatClient.prompt()
                .system(getSystemPrompt())
                .user(prompt)
                .call()
                .content();

        log.info("[BudgetAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Budget analysis could not be generated.";
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }

    @Override
    protected String buildUserPrompt(AgentContext context) {
        String plan = context.getShared("planner_output");
        String tripDetail = context.getShared("trip_detail");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";
        String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";

        return """
                Here is the trip plan from the Planner Agent:
                %s

                Current trip details:
                %s

                User Preferences (personalize budget allocations):
                %s

                User's original request: %s

                Based on this information, provide a detailed budget breakdown
                with recommended allocations for each category.
                Adjust allocations based on user preferences (e.g., foodie = higher dining budget).
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);
    }
}
