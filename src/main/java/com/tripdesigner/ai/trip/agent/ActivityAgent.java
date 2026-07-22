package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.TripPlanningTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Activity Agent（活动安排 Agent）。
 *
 * 工作流中的第七个 Agent，也是最重要的执行 Agent 之一。
 * 综合前面所有 Agent 的输出（计划、交通、餐饮、景点、住宿、预算），
 * 生成每日详细的活动时间表。
 *
 * 输出内容包括：
 * - 每日时间线（上午/午餐/下午/晚餐/晚上）
 * - 每个活动的名称、地点、时间、类别
 * - 活动之间的交通时间
 * - 预留的休息缓冲时间
 *
 * 设计原则：劳逸结合，不过度安排，匹配用户旅行风格。
 */
@Slf4j
@Component
public class ActivityAgent extends AbstractAgent {

    /**
     * 系统提示词：定义活动安排专家的角色。
     * 要求按天创建详细的时间表，包含活动名称、位置、时间、类别和描述。
     */
    private static final String SYSTEM_PROMPT = """
            You are an activity planning specialist for travel.
            Your job is to create detailed daily activity schedules for the trip.

            Given the trip details (destinations, dates, budget, interests, agent recommendations),
            you should:
            1. First, use getTripDetail to get the current trip information including its ID
            2. For each day of the trip, use addTripDay to create a day record (if not already created)
            3. For each activity within a day, use addTripActivity to add specific activities
            4. Output the complete daily schedule

            CRITICAL - You MUST persist all activities using the available tools:
            - Use addTripDay to create each day of the trip
            - Use addTripActivity to add each activity (sightseeing, dining, transport, etc.)
            - All activities from previous agents' recommendations should be included
            - Categorize activities properly: sightseeing, dining, transport, accommodation, shopping, other

            When adding trip days:
            - tripId: the ID of the trip (get from getTripDetail)
            - dayNumber: sequential day number (1, 2, 3...)
            - date: the date for this day (yyyy-mm-dd)
            - title: day theme or focus

            When adding activities:
            - tripDayId: the ID of the trip day (get from addTripDay result)
            - name: activity name
            - startTime/endTime: in HH:mm format
            - category: sightseeing/dining/transport/accommodation/shopping/other
            - place: location name
            - description: brief description

            Format each day clearly with time slots, activity names, locations, and descriptions.
            Balance the schedule - don't overpack. Include downtime.
            Match activities to the user's interests and travel style.
            """;

    private final TripPlanningTools tripPlanningTools;

    public ActivityAgent(ChatClient chatClient, ObjectMapper objectMapper,
                         @Lazy TripPlanningTools tripPlanningTools) {
        super(chatClient, objectMapper);
        this.tripPlanningTools = tripPlanningTools;
    }

    @Override
    public String getName() {
        return "activity";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 执行 Activity Agent：
     * 综合所有前置 Agent 的输出（planner, sightseeing, dining 等），
     * 为行程的每一天生成详细的活动时间表。
     *
     * @param context 共享上下文
     * @return 每日活动安排文本
     */
    @Override
    public String execute(AgentContext context) {
        String plan = context.getShared("planner_output");
        String tripDetail = context.getShared("trip_detail");
        String sightseeing = context.getShared("sightseeing_output");
        String dining = context.getShared("dining_output");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";
        String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";
        String tripMemorySummary = context.getTripMemorySummary() != null ? context.getTripMemorySummary() : "No past trip memories available.";

        String prompt = """
                Here is the trip plan from the Planner Agent:
                %s

                Current trip details:
                %s

                Sightseeing recommendations:
                %s

                Dining recommendations:
                %s

                User Preferences:
                %s

                Past Trip Memories:
                %s

                User's original request: %s

                Based on all this information, create detailed daily activity schedules
                with specific times, locations, and descriptions. Build in rest time.
                Personalize based on user preferences and past experiences.
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                sightseeing != null ? sightseeing : "No sightseeing output available",
                dining != null ? dining : "No dining output available",
                preferenceSummary,
                tripMemorySummary,
                userRequest);

        String result = chatClient.prompt()
                .system(getSystemPrompt())
                .user(prompt)
                .tools(tripPlanningTools)
                .call()
                .content();

        log.info("[ActivityAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Activity schedule could not be generated.";
    }

    @Override
    public Object[] getTools() {
        return new Object[]{tripPlanningTools};
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }

    @Override
    protected String buildUserPrompt(AgentContext context) {
        String plan = context.getShared("planner_output");
        String tripDetail = context.getShared("trip_detail");
        String sightseeing = context.getShared("sightseeing_output");
        String dining = context.getShared("dining_output");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";
        String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";
        String tripMemorySummary = context.getTripMemorySummary() != null ? context.getTripMemorySummary() : "No past trip memories available.";

        return """
                Here is the trip plan from the Planner Agent:
                %s

                Current trip details:
                %s

                Sightseeing recommendations:
                %s

                Dining recommendations:
                %s

                User Preferences:
                %s

                Past Trip Memories:
                %s

                User's original request: %s

                Based on all this information, create detailed daily activity schedules
                with specific times, locations, and descriptions. Build in rest time.
                Personalize based on user preferences and past experiences.
                """.formatted(plan != null ? plan : "No planner output available",
                tripDetail != null ? tripDetail : "No trip details available",
                sightseeing != null ? sightseeing : "No sightseeing output available",
                dining != null ? dining : "No dining output available",
                preferenceSummary,
                tripMemorySummary,
                userRequest);
    }
}
