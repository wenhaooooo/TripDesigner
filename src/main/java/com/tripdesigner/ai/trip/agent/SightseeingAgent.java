package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.TripPlanningTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Sightseeing Agent（景点 Agent）。
 *
 * 工作流中的第四个 Agent，负责推荐景点和旅游地标。
 * 基于 Planner 输出的目的地和用户兴趣，
 * 推荐必看景点、隐藏宝藏、博物馆、自然风光等。
 *
 * 推荐策略：热门景点 + 小众秘境混合推荐，
 * 根据用户兴趣偏好调整优先级。
 */
@Slf4j
@Component
public class SightseeingAgent extends AbstractAgent {

    /**
     * 系统提示词：定义景点推荐专家的角色。
     * 要求包含门票信息、建议游览时长、最佳到访时间等实用信息。
     */
    private static final String SYSTEM_PROMPT = """
            You are a sightseeing specialist for travel planning.
            Your job is to recommend the best attractions, landmarks, and tourist destinations.

            Given the trip details (destination, dates, budget, interests),
            you should recommend:
            1. Must-see attractions and landmarks
            2. Hidden gems and local favorites
            3. Museum/gallery recommendations
            4. Natural spots (parks, beaches, mountains)
            5. Entrance fees and opening hours
            6. Suggested duration for each visit

            IMPORTANT - You MUST persist your recommendations using the available tools:
            1. First, use getTripDetail to get the current trip information including its ID
            2. If the trip doesn't have day records yet, use addTripDay to create them for each day
            3. For each sightseeing recommendation, use addTripActivity with category='sightseeing'
               to add it to the appropriate trip day

            Format your recommendations clearly with:
            - Attraction name
            - Location/area
            - Entrance fee (if any)
            - Suggested duration
            - Why it's worth visiting
            - Best time to visit

            Prioritize based on the user's interests. Include a mix of popular and niche spots.
            """;

    private final TripPlanningTools tripPlanningTools;

    public SightseeingAgent(ChatClient chatClient, ObjectMapper objectMapper,
                            @Lazy TripPlanningTools tripPlanningTools) {
        super(chatClient, objectMapper);
        this.tripPlanningTools = tripPlanningTools;
    }

    @Override
    public String getName() {
        return "sightseeing";
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
     * 执行 Sightseeing Agent：
     * 读取 Planner 的行程计划，结合用户兴趣偏好，
     * 为每个目的地推荐景点和游览项目。
     *
     * @param context 共享上下文
     * @return 景点推荐列表文本
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

                User Preferences (personalize sightseeing recommendations):
                %s

                User's original request: %s

                Based on this information, recommend sightseeing destinations and attractions
                for each day of the trip. Include must-sees and hidden gems.
                Prioritize based on user interests and preferences.
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

        log.info("[SightseeingAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Sightseeing recommendations could not be generated.";
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

                User Preferences (personalize sightseeing recommendations):
                %s

                User's original request: %s

                Based on this information, recommend sightseeing destinations and attractions
                for each day of the trip. Include must-sees and hidden gems.
                Prioritize based on user interests and preferences.
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                preferenceSummary,
                userRequest);
    }
}
