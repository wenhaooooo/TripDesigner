package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.TripPlanningTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Transport Agent（交通 Agent）。
 *
 * 工作流中的第二个 Agent，负责推荐交通方案。
 * 在 Planner Agent 确定了目的地和天数后，
 * 此 Agent 提供城际交通（飞机/火车/巴士）和当地交通建议。
 *
 * 推荐内容：
 * - 城际交通方式（航班、高铁、巴士、自驾）
 * - 当地交通（出租车、公交、共享单车、步行）
 * - 预估费用范围
 * - 行程时间和便利性考量
 */
@Slf4j
@Component
public class TransportAgent extends AbstractAgent {

    /**
     * 系统提示词：定义交通专家的角色和行为。
     * 要求考虑预算、便利性、环保和当地条件等因素。
     */
    private static final String SYSTEM_PROMPT = """
            You are a transportation specialist for travel planning.
            Your job is to recommend the best transportation options for each leg of the trip.

            Given the trip details (destinations, dates, budget, travel style),
            you should recommend:
            1. Inter-city transportation (flights, trains, buses, cars)
            2. Local transportation (taxis, public transit, bikes, walking)
            3. Estimated costs for each transportation option
            4. Travel times and convenience factors

            IMPORTANT - You MUST persist your recommendations using the available tools:
            1. First, use getTripDetail to get the current trip information including its ID
            2. If the trip doesn't have day records yet, use addTripDay to create them for each day
            3. For each transport recommendation, use addTripActivity with category='transport'
               to add it to the appropriate trip day

            Format your recommendations clearly with:
            - From/To locations
            - Recommended transport mode
            - Estimated cost range
            - Travel time
            - Why this option is recommended

            Consider: budget constraints, convenience, environmental impact, and local conditions.
            Provide realistic, practical advice.
            """;

    private final TripPlanningTools tripPlanningTools;

    public TransportAgent(ChatClient chatClient, ObjectMapper objectMapper,
                          @Lazy TripPlanningTools tripPlanningTools) {
        super(chatClient, objectMapper);
        this.tripPlanningTools = tripPlanningTools;
    }

    @Override
    public String getName() {
        return "transport";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public Object[] getTools() {
        return new Object[]{tripPlanningTools};
    }

    /**
     * 执行 Transport Agent：
     * 读取 Planner 输出的行程计划，结合用户偏好，
     * 为每个行程段推荐合适的交通方案。
     *
     * @param context 共享上下文（包含 planner_output、用户偏好等）
     * @return 交通推荐方案文本
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

                User Preferences (personalize transport recommendations):
                %s

                User's original request: %s

                Based on this information, recommend the best transportation options
                for each leg of the trip. Include inter-city and local transportation.
                Personalize based on user preferences (e.g., comfort level, speed vs. budget).
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);

        String result = chatClient.prompt()
                .system(getSystemPrompt())
                .user(prompt)
                .tools(tripPlanningTools)
                .call()
                .content();

        log.info("[TransportAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Transport recommendations could not be generated.";
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

                User Preferences (personalize transport recommendations):
                %s

                User's original request: %s

                Based on this information, recommend the best transportation options
                for each leg of the trip. Include inter-city and local transportation.
                Personalize based on user preferences (e.g., comfort level, speed vs. budget).
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);
    }
}
