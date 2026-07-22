package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 所有 Agent 的抽象基类。
 *
 * 提供了 Agent 工作所需的基础设施：
 * - ChatClient：与 LLM（大语言模型）进行交互的客户端
 * - ObjectMapper：序列化 Agent 上下文用于日志记录
 *
 * 子类需要实现三个核心方法：
 * - getName()：Agent 的唯一标识名称
 * - getSystemPrompt()：定义 Agent 角色和行为的系统提示词
 * - execute()：Agent 的核心执行逻辑
 *
 * Agent 工作流模式：
 * 1. 系统提示词（System Prompt）定义 Agent 的专业角色
 * 2. 用户提示词（User Prompt）包含具体任务上下文
 * 3. LLM 根据提示词生成回复，可能附带工具调用
 * 4. Agent 将回复传递给下一个 Agent 或保存到数据库
 */
@Slf4j
public abstract class AbstractAgent {

    /** Spring AI ChatClient，用于与 LLM 通信 */
    protected final ChatClient chatClient;

    /** Jackson ObjectMapper，用于 JSON 序列化 */
    protected final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    protected AbstractAgent(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取 Agent 的唯一名称。
     * 用于在工作流中标识 Agent，也作为 sharedData 中的 key 前缀。
     *
     * @return Agent 名称（如 "planner", "transport", "dining"）
     */
    public abstract String getName();

    /**
     * 获取 Agent 的系统提示词（System Prompt）。
     * 定义了 Agent 的角色、职责、输出格式要求等。
     * LLM 会根据 System Prompt 来"扮演"对应的专业角色。
     *
     * @return 系统提示词字符串
     */
    public abstract String getSystemPrompt();

    /**
     * 获取带有语言和时间信息的增强系统提示词。
     * 自动注入当前日期和用户语言，确保所有 Agent 输出语言一致。
     *
     * @param context Agent 上下文
     * @return 增强后的系统提示词
     */
    public String getEnhancedSystemPrompt(AgentContext context) {
        String basePrompt = getSystemPrompt();
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        int currentYear = LocalDate.now().getYear();
        String userLanguage = context.getUserLanguage();
        
        if (userLanguage == null || userLanguage.isEmpty()) {
            userLanguage = "zh";
        }
        
        String languageName = getLanguageName(userLanguage);
        
        String enhancement = """
            
            === SYSTEM CONSTRAINTS ===
            Current date: %s (year %d)
            User language: %s
            
            IMPORTANT RULES:
            1. RESPONSE LANGUAGE: Your response MUST be in %s, matching the user's language.
               Use this language for all explanations, descriptions, and comments.
               Proper nouns (city names, attraction names, food names) can remain in their original language.
            2. DATE VALIDATION: All dates mentioned in your response MUST be in the FUTURE (year %d or later).
               Never use past dates like 2024, 2025 or earlier.
            3. CONSISTENCY: Maintain consistent language throughout your entire response.
            """.formatted(currentDate, currentYear, languageName, languageName, currentYear);
        
        return basePrompt + enhancement;
    }

    private String getLanguageName(String code) {
        return switch (code) {
            case "zh" -> "Chinese (中文)";
            case "en" -> "English";
            case "ja" -> "Japanese (日本語)";
            case "ko" -> "Korean (한국어)";
            default -> "English";
        };
    }

    /**
     * 执行 Agent 的核心业务逻辑。
     * 子类在此方法中构建 prompt、调用 LLM、处理返回结果。
     *
     * @param context 共享的 Agent 上下文（包含用户请求、其他 Agent 的输出等）
     * @return Agent 生成的文本输出
     */
    public abstract String execute(AgentContext context);

    /**
     * 流式执行 Agent 的核心业务逻辑。
     * 将 LLM 生成的内容逐块推送给回调函数，实现实时输出。
     * 
     * 注意：流式模式下工具调用不会自动执行，需要在非流式模式下调用 execute() 来处理工具调用。
     * 本方法主要用于实时显示 Agent 的输出内容。
     *
     * @param context 共享的 Agent 上下文
     * @param contentCallback 内容回调，每次收到新内容块时调用
     * @return Agent 生成的完整文本输出
     */
    public String streamExecute(AgentContext context, Consumer<String> contentCallback) {
        StringBuilder fullResponse = new StringBuilder();

        Object[] tools = getTools();
        boolean hasTools = tools != null && tools.length > 0;

        if (hasTools) {
            String result = execute(context);
            contentCallback.accept(result);
            fullResponse.append(result);
        } else {
            chatClient.prompt()
                    .system(getEnhancedSystemPrompt(context))
                    .user(buildUserPrompt(context))
                    .stream()
                    .content()
                    .doOnNext(content -> {
                        if (content != null && !content.isEmpty()) {
                            fullResponse.append(content);
                            contentCallback.accept(content);
                        }
                    })
                    .blockLast();
        }

        return fullResponse.toString();
    }

    /**
     * 构建用户提示词。
     * 默认使用 context.getUserRequest()，子类可重写以添加更多上下文信息。
     *
     * @param context Agent 上下文
     * @return 用户提示词字符串
     */
    protected String buildUserPrompt(AgentContext context) {
        return context.getUserRequest();
    }

    /**
     * 获取该 Agent 可用的工具列表。
     * 子类可重写此方法以注册 Function Calling 工具。
     * 默认返回空数组（无工具）。
     *
     * @return 工具对象数组
     */
    public Object[] getTools() {
        return new Object[0];
    }

    /**
     * 获取最大重试次数。
     * 当 Agent 调用 LLM 失败时，工作流引擎会根据此值进行重试。
     * 默认 3 次，子类可重写调整。
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return 3;
    }

    /**
     * 将 AgentContext 序列化为 JSON 字符串，用于日志记录。
     * 方便调试时查看 Agent 接收到的完整上下文信息。
     *
     * @param context Agent 上下文
     * @return JSON 字符串（序列化失败时返回简化的描述）
     */
    protected String contextToJson(AgentContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "AgentContext: " + context.getUserRequest();
        }
    }
}
