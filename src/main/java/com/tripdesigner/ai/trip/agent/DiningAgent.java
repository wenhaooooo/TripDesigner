package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.TripPlanningTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Dining Agent（餐饮 Agent）。
 *
 * 工作流中的第三个 Agent，负责推荐餐饮和美食体验。
 * 基于 Planner 输出的行程计划和用户饮食偏好，
 * 推荐特色菜品、餐厅、价格范围和最佳用餐时间。
 *
 * 个性化亮点：如果用户偏好设为"美食爱好者"类型，
 * 此 Agent 会增加餐饮预算比例，推荐更多特色餐厅。
 */
@Slf4j
@Component
public class DiningAgent extends AbstractAgent {

    /**
     * 系统提示词：定义餐饮专家的角色。
     * 要求推荐地道本地美食，包含从街头小吃到精致餐厅的多样化选择。
     */
    private static final String SYSTEM_PROMPT = """
            You are a dining specialist for travel planning.
            Your job is to recommend the best restaurants and dining experiences for the trip.

            Given the trip details (destination, dates, budget, interests),
            you should recommend:
            1. Signature local dishes to try
            2. Restaurant recommendations by area/day
            3. Price ranges for each recommendation
            4. Special dining experiences (street food, fine dining, cooking classes, etc.)
            5. Dietary considerations if mentioned

            IMPORTANT - You MUST persist your recommendations using the available tools:
            1. First, use getTripDetail to get the current trip information including its ID
            2. If the trip doesn't have day records yet, use addTripDay to create them for each day
            3. For each dining recommendation, use addTripActivity with category='dining'
               to add it to the appropriate trip day

            Format your recommendations clearly with:
            - Restaurant/food name
            - Location/area
            - Price range
            - Why it's recommended
            - Best day/time to visit

            Focus on authentic, local experiences. Include a mix of casual and special dining.
            """;

    private final TripPlanningTools tripPlanningTools;

    public DiningAgent(ChatClient chatClient, ObjectMapper objectMapper,
                       @Lazy TripPlanningTools tripPlanningTools) {
        super(chatClient, objectMapper);
        this.tripPlanningTools = tripPlanningTools;
    }

    @Override
    public String getName() {
        return "dining";
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
     * 执行 Dining Agent：
     * 读取 Planner 输出的行程计划和用户饮食偏好，
     * 为每天的行程推荐合适的餐饮选项。
     *
     * @param context 共享上下文
     * @return 餐饮推荐列表文本
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

                User Preferences (personalize food recommendations):
                %s

                User's original request: %s

                Based on this information, recommend restaurants and dining experiences
                for each day of the trip. Include local specialties and price ranges.
                Personalize based on user preferences (dietary preferences, food types, etc.).
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

        log.info("[DiningAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Dining recommendations could not be generated.";
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

                User Preferences (personalize food recommendations):
                %s

                User's original request: %s

                Based on this information, recommend restaurants and dining experiences
                for each day of the trip. Include local specialties and price ranges.
                Personalize based on user preferences (dietary preferences, food types, etc.).
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);
    }
}
