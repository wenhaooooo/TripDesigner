package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Reflection Agent（复盘 Agent）。
 *
 * 工作流中的最后一个 Agent（第八个），负责审查和完善整个行程计划。
 * 在收到所有前置 Agent 的输出后，进行全面的质量审查：
 *
 * 1. 整体连贯性检查：活动流程是否合理、时间是否冲突、预算是否匹配
 * 2. 问题识别：日程过满、遗漏机会、季节性考量、天气预案
 * 3. 改进建议：活动重排、增减项目、预算调整、替代方案
 * 4. 最终摘要：行程亮点、重要提醒、行前建议
 *
 * 特色功能：输出中包含 "Memories to Remember" 部分，
 * 自动发现用户偏好并格式化为可持久化的记忆数据。
 */
@Slf4j
@Component
public class ReflectionAgent extends AbstractAgent {

    /**
     * 系统提示词：定义旅行计划评审专家的角色。
     * 要求评估整体连贯性、识别问题、提出改进建议，并提供最终行程摘要。
     */
    private static final String SYSTEM_PROMPT = """
            You are a travel plan reviewer and improvement specialist.
            Your job is to review the complete trip plan and suggest improvements.

            Given the outputs from all agents (planner, transport, dining, sightseeing,
            accommodation, budget, activity), you should:

            1. Review the overall coherence of the plan:
               - Do the activities flow logically?
               - Are there any conflicts or unrealistic timings?
               - Is the budget realistic for the planned activities?

            2. Identify potential issues:
               - Over-scheduled days
               - Missed opportunities
               - Seasonal considerations
               - Weather contingencies

            3. Suggest specific improvements:
               - Reordering activities for better flow
               - Adding or removing activities
               - Budget adjustments
               - Alternative options

            4. Provide a final summary:
               - Key highlights of the trip
               - Important reminders for the traveler
               - Packing suggestions

            Format your output with:
            - Overall assessment (1-2 paragraphs)
            - Specific issues identified
            - Improvement suggestions
            - Final trip summary
            - Traveler tips
            """;

    public ReflectionAgent(ChatClient chatClient, ObjectMapper objectMapper) {
        super(chatClient, objectMapper);
    }

    @Override
    public String getName() {
        return "reflection";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 执行 Reflection Agent：
     * 读取所有前置 Agent 的输出，进行全面审查和改进建议。
     *
     * 特殊输出格式：
     * 在输出末尾包含 "MEMORY:" 前缀的偏好发现记录，
     * 这些记录会被工作流引擎解析并持久化到 TripMemory 表中，
     * 用于未来行程的个性化推荐。
     *
     * @param context 共享上下文
     * @return 完整的行程审查和改进建议文本
     */
    @Override
    public String execute(AgentContext context) {
        String plan = context.getShared("planner_output");
        String transport = context.getShared("transport_output");
        String dining = context.getShared("dining_output");
        String sightseeing = context.getShared("sightseeing_output");
        String accommodation = context.getShared("accommodation_output");
        String budget = context.getShared("budget_output");
        String activity = context.getShared("activity_output");
        String tripDetail = context.getShared("trip_detail");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";

        String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";
        String tripMemorySummary = context.getTripMemorySummary() != null ? context.getTripMemorySummary() : "No past trip memories available.";

        String prompt = """
                Here is the complete trip plan from all agents:

                === PLANNER OUTPUT ===
                %s

                === TRANSPORT RECOMMENDATIONS ===
                %s

                === DINING RECOMMENDATIONS ===
                %s

                === SIGHTSEEING RECOMMENDATIONS ===
                %s

                === ACCOMMODATION RECOMMENDATIONS ===
                %s

                === BUDGET BREAKDOWN ===
                %s

                === DAILY ACTIVITY SCHEDULE ===
                %s

                === CURRENT TRIP DETAILS ===
                %s

                === USER PREFERENCES ===
                %s

                === PAST TRIP MEMORIES ===
                %s

                User's original request: %s

                Review this complete plan and provide:
                1. Overall assessment of the trip plan
                2. Any issues or conflicts you identified
                3. Specific improvement suggestions
                4. Final trip summary with key highlights
                5. Important traveler tips and reminders

                At the end, include a "Memories to Remember" section with 2-3 key insights
                about user preferences discovered during this planning, formatted as:
                MEMORY: PREFERENCE_DISCOVERED - <content>
                MEMORY: HIGHLIGHT - <content>
                """.formatted(plan != null ? plan : "N/A",
                transport != null ? transport : "N/A",
                dining != null ? dining : "N/A",
                sightseeing != null ? sightseeing : "N/A",
                accommodation != null ? accommodation : "N/A",
                budget != null ? budget : "N/A",
                activity != null ? activity : "N/A",
                tripDetail != null ? tripDetail : "N/A",
                preferenceSummary,
                tripMemorySummary,
                userRequest);

        String result = chatClient.prompt()
                .system(getSystemPrompt())
                .user(prompt)
                .call()
                .content();

        log.info("[ReflectionAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Plan review could not be generated.";
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }

    @Override
    protected String buildUserPrompt(AgentContext context) {
        String plan = context.getShared("planner_output");
        String transport = context.getShared("transport_output");
        String dining = context.getShared("dining_output");
        String sightseeing = context.getShared("sightseeing_output");
        String accommodation = context.getShared("accommodation_output");
        String budget = context.getShared("budget_output");
        String activity = context.getShared("activity_output");
        String tripDetail = context.getShared("trip_detail");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";
        String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";
        String tripMemorySummary = context.getTripMemorySummary() != null ? context.getTripMemorySummary() : "No past trip memories available.";

        return """
                Here is the complete trip plan from all agents:

                === PLANNER OUTPUT ===
                %s

                === TRANSPORT RECOMMENDATIONS ===
                %s

                === DINING RECOMMENDATIONS ===
                %s

                === SIGHTSEEING RECOMMENDATIONS ===
                %s

                === ACCOMMODATION RECOMMENDATIONS ===
                %s

                === BUDGET BREAKDOWN ===
                %s

                === DAILY ACTIVITY SCHEDULE ===
                %s

                === CURRENT TRIP DETAILS ===
                %s

                === USER PREFERENCES ===
                %s

                === PAST TRIP MEMORIES ===
                %s

                User's original request: %s

                Review this complete plan and provide:
                1. Overall assessment of the trip plan
                2. Any issues or conflicts you identified
                3. Specific improvement suggestions
                4. Final trip summary with key highlights
                5. Important traveler tips and reminders

                At the end, include a "Memories to Remember" section with 2-3 key insights
                about user preferences discovered during this planning, formatted as:
                MEMORY: PREFERENCE_DISCOVERED - <content>
                MEMORY: HIGHLIGHT - <content>
                """.formatted(plan != null ? plan : "N/A",
                transport != null ? transport : "N/A",
                dining != null ? dining : "N/A",
                sightseeing != null ? sightseeing : "N/A",
                accommodation != null ? accommodation : "N/A",
                budget != null ? budget : "N/A",
                activity != null ? activity : "N/A",
                tripDetail != null ? tripDetail : "N/A",
                preferenceSummary,
                tripMemorySummary,
                userRequest);
    }
}
