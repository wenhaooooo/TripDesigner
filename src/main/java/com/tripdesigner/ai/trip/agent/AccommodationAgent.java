package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.TripPlanningTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Accommodation Agent（住宿 Agent）。
 *
 * 工作流中的第五个 Agent，负责推荐住宿选项。
 * 基于目的地、预算和旅行风格，推荐酒店/民宿/青旅等住宿方案。
 *
 * 推荐考量因素：
 * - 预算匹配度
 * - 地理位置便利性（靠近景点/交通/餐饮）
 * - 设施和服务
 * - 用户住宿风格偏好（经济型/舒适型/豪华型）
 */
@Slf4j
@Component
public class AccommodationAgent extends AbstractAgent {

    /**
     * 系统提示词：定义住宿专家的角色。
     * 要求提供各区域的住宿推荐，包含价格范围、设施和预订建议。
     */
    private static final String SYSTEM_PROMPT = """
            You are an accommodation specialist for travel planning.
            Your job is to recommend the best places to stay for the trip.

            Given the trip details (destination, dates, budget, travel style),
            you should recommend:
            1. Hotel/hostel/guesthouse recommendations by area
            2. Price ranges per night
            3. Key amenities and features
            4. Location advantages (near attractions, transit, food)
            5. Booking tips (when to book, cancellation policies)

            IMPORTANT - You MUST persist your recommendations using the available tools:
            1. First, use getTripDetail to get the current trip information including its ID
            2. If the trip doesn't have day records yet, use addTripDay to create them for each day
            3. For each accommodation recommendation, use addTripActivity with category='accommodation'
               to add it to the appropriate trip day

            Format your recommendations clearly with:
            - Accommodation name/type
            - Location/area
            - Price range per night
            - Key amenities
            - Why it's recommended

            Consider the user's budget, travel style, and location preferences.
            Provide a range of options from budget to mid-range.
            """;

    private final TripPlanningTools tripPlanningTools;

    public AccommodationAgent(ChatClient chatClient, ObjectMapper objectMapper,
                              @Lazy TripPlanningTools tripPlanningTools) {
        super(chatClient, objectMapper);
        this.tripPlanningTools = tripPlanningTools;
    }

    @Override
    public String getName() {
        return "accommodation";
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
     * 执行 Accommodation Agent：
     * 读取 Planner 的行程计划，结合预算和偏好，
     * 为每个目的地推荐合适的住宿方案。
     *
     * @param context 共享上下文
     * @return 住宿推荐列表文本
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

                User Preferences (personalize accommodation):
                %s

                User's original request: %s

                Based on this information, recommend accommodation options
                for each area of the trip. Include price ranges and key amenities.
                Personalize based on user preferences (hotel type, location preferences, etc.).
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

        log.info("[AccommodationAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Accommodation recommendations could not be generated.";
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

                User Preferences (personalize accommodation):
                %s

                User's original request: %s

                Based on this information, recommend accommodation options
                for each area of the trip. Include price ranges and key amenities.
                Personalize based on user preferences (hotel type, location preferences, etc.).
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);
    }
}
