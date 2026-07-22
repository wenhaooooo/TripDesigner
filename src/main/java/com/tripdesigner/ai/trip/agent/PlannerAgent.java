package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.TripPlanningTools;
import com.tripdesigner.ai.trip.SystemTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Planner Agent（规划 Agent）。
 *
 * 工作流中的第一个 Agent，负责：
 * 1. 分析用户的旅行需求描述
 * 2. 提取关键信息（天数、目的地、预算、兴趣等）
 * 3. 生成结构化计划供后续 Agent 使用
 * 4. 结合用户偏好和过往旅行记忆进行个性化规划
 *
 * 输出格式为结构化的规划摘要，包含：
 * - days_count: 行程天数
 * - destinations: 目的地列表
 * - budget_notes: 预算分配建议
 * - interests: 兴趣/活动列表
 * - travel_style: 旅行风格偏好
 */
@Slf4j
@Component
public class PlannerAgent extends AbstractAgent {

    /**
     * Planner Agent 的系统提示词。
     * 定义 Agent 作为旅行规划协调员的角色，
     * 要求输出结构化信息供其他专业 Agent 使用。
     */
    private String buildSystemPrompt() {
        String currentDate = java.time.LocalDate.now().toString();
        int currentYear = java.time.LocalDate.now().getYear();
        
        return """
            You are a trip planning coordinator. Your job is to analyze the user's travel request
            and decompose it into a structured plan for other specialized agents.

            Given the user's request, you should:
            1. Analyze the user's travel request
            2. Extract key information (days, destinations, budget, interests, etc.)
            3. Use the createTrip tool to create a new trip record with the basic information
            4. Output a structured plan for other agents to follow

            Format your output as a clear, structured response that other agents can use.
            Be concise but comprehensive. Focus on planning decisions, not detailed itineraries.

            IMPORTANT - Current date: %s (year %d)
            When creating a trip:
            - title: use a descriptive name like "{Destination} {Days} Day Trip"
            - destinationName: the main destination
            - startDate/endDate: MUST be in the FUTURE (year %d or later). If user doesn't specify dates,
              use dates starting from next week or next month. NEVER use past dates like 2024 or earlier.
              Example: if current date is 2026-07-21, valid dates are 2026-07-28 onwards.
            - budget: if mentioned, use the specified amount
            - description: brief overview of the trip

            Output should include:
            - days_count: integer
            - destinations: list of destination names
            - budget_notes: budget allocation suggestions
            - interests: list of key interests/activities
            - travel_style: description of preferred pace
            """.formatted(currentDate, currentYear, currentYear);
    }

    private final TripPlanningTools tripPlanningTools;
    private final SystemTools systemTools;

    public PlannerAgent(ChatClient chatClient, ObjectMapper objectMapper,
                        @Lazy TripPlanningTools tripPlanningTools,
                        @Lazy SystemTools systemTools) {
        super(chatClient, objectMapper);
        this.tripPlanningTools = tripPlanningTools;
        this.systemTools = systemTools;
    }

    @Override
    public String getName() {
        return "planner";
    }

    @Override
    public String getSystemPrompt() {
        return buildSystemPrompt();
    }

    /**
     * 执行 Planner Agent 的核心逻辑。
     * 1. 获取用户请求、偏好摘要和旅行记忆
     * 2. 构建提示词，要求 LLM 分析需求并生成结构化计划
     * 3. 偏好摘要和记忆用于实现个性化推荐
     * 4. 使用工具调用创建行程
     *
     * @param context 共享上下文（包含用户输入和其他 Agent 的中间结果）
     * @return 结构化的旅行计划文本
     */
    @Override
    public String execute(AgentContext context) {
        String prompt = buildUserPrompt(context);

        String result = chatClient.prompt()
                .system(getEnhancedSystemPrompt(context))
                .user(prompt)
                .tools(tripPlanningTools, systemTools)
                .call()
                .content();

        log.info("[PlannerAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Planning analysis could not be generated.";
    }

    @Override
    public Object[] getTools() {
        return new Object[]{tripPlanningTools, systemTools};
    }

    @Override
    protected String buildUserPrompt(AgentContext context) {
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";
        String conversationHistory = context.getConversationHistory() != null ? context.getConversationHistory() : "No prior conversation.";

        // 方案1：优先使用 RAG 语义检索的记忆（更精准），回退到全量记忆摘要
        String memoryContext = context.getRagMemoryContext();
        if (memoryContext == null || memoryContext.isBlank()) {
            String preferenceSummary = context.getPreferenceSummary() != null ? context.getPreferenceSummary() : "No user preferences available.";
            String tripMemorySummary = context.getTripMemorySummary() != null ? context.getTripMemorySummary() : "No past trip memories available.";
            memoryContext = """
                    User Preferences (personalize the plan based on these):
                    %s

                    Past Trip Memories (learn from these experiences):
                    %s""".formatted(preferenceSummary, tripMemorySummary);
        }

        // 方案2：注入目的地知识（RAG 检索的知识库内容）
        String knowledgeContext = context.getRagKnowledgeContext() != null
                ? context.getRagKnowledgeContext()
                : "No destination knowledge available.";

        return """
                === CONVERSATION HISTORY ===
                %s

                Here is the user's travel request:
                %s

                === PERSONALIZATION CONTEXT (RAG-enhanced) ===
                %s

                === DESTINATION KNOWLEDGE (RAG-retrieved, use for accurate recommendations) ===
                %s

                Analyze this request and provide a structured plan for the other agents to follow.
                Take user preferences and past experiences into account when planning.
                Use destination knowledge to provide accurate, up-to-date recommendations.
                If conversation history exists, use it to understand context and follow up on prior requests.
                """.formatted(conversationHistory, userRequest, memoryContext, knowledgeContext);
    }

    /**
     * 截断长字符串用于日志输出（避免日志过于冗长）。
     *
     * @param s   原始字符串
     * @param max 最大长度
     * @return 截断后的字符串（过长时添加 "..." 后缀）
     */
    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }
}
