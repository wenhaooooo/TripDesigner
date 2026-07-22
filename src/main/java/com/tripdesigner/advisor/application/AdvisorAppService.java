package com.tripdesigner.advisor.application;

import com.tripdesigner.advisor.api.vo.AdvisorResponse;
import com.tripdesigner.ai.rag.RagMemoryService;
import com.tripdesigner.ai.rag.RagService;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.dto.CreateConversationRequest;
import com.tripdesigner.conversation.api.vo.ConversationVo;
import com.tripdesigner.conversation.application.ConversationAppService;
import com.tripdesigner.conversation.domain.ConversationRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 旅行顾问应用服务。
 *
 * 使用 Spring AI ChatClient + RAG 知识库回答用户的旅行咨询问题。
 * 支持同步调用和流式调用两种模式，所有对话消息通过 ConversationAppService 持久化。
 *
 * RAG 上下文构建：
 * - RagService：检索旅行知识库中的相关文档
 * - RagMemoryService：检索与用户请求相关的偏好和历史旅行记忆
 *
 * 对话管理：
 * - 若 conversationId 为 null，自动创建新对话（标题从问题中截取）
 * - 用户问题和 AI 回答均保存到对话消息表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisorAppService {

    private final ChatClient chatClient;
    private final ConversationAppService conversationAppService;
    private final RagService ragService;
    private final RagMemoryService ragMemoryService;

    /**
     * 系统提示词：定义旅行顾问的角色和能力范围。
     */
    private static final String SYSTEM_PROMPT = """
            You are a professional travel advisor assistant. You help users with:
            1. Visa and entry requirements
            2. Local culture and customs
            3. Transportation options
            4. Weather and best travel times
            5. Safety tips and emergency information
            6. Currency and payment methods
            7. Language and communication tips

            Provide accurate, helpful, and concise answers. If you're not sure about something, say so.
            When relevant, mention that the user can use the trip planning feature for detailed itinerary generation.
            """;

    private static final int RAG_TOP_K = 5;
    private static final int TITLE_MAX_LENGTH = 64;

    /**
     * 同步回答用户的旅行问题。
     *
     * @param userId         用户 ID
     * @param question       用户问题
     * @param conversationId 对话 ID（为 null 时自动创建新对话）
     * @return 包含回答和对话 ID 的响应
     */
    public AdvisorResponse ask(Long userId, String question, Long conversationId) {
        log.info("[AdvisorAppService] ask: userId={}, convId={}, questionLen={}",
                userId, conversationId, question != null ? question.length() : 0);

        Long convId = ensureConversation(userId, question, conversationId);

        saveMessage(convId, ConversationRole.USER, question);

        String userPrompt = buildUserPrompt(userId, question);

        String answer;
        try {
            answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[AdvisorAppService] ChatClient call failed: {}", e.getMessage(), e);
            throw new BizException(ResultCode.AI_GENERATION_FAILED, e.getMessage());
        }

        if (answer == null || answer.isBlank()) {
            log.warn("[AdvisorAppService] Empty answer from ChatClient");
            throw new BizException(ResultCode.AI_GENERATION_FAILED, "empty response from AI");
        }

        saveMessage(convId, ConversationRole.ASSISTANT, answer);

        log.info("[AdvisorAppService] ask completed: convId={}, answerLen={}", convId, answer.length());
        return AdvisorResponse.builder()
                .answer(answer)
                .conversationId(convId)
                .build();
    }

    /**
     * 流式回答用户的旅行问题。
     *
     * 通过 callback 逐块推送 LLM 生成的内容，流结束后保存完整的 AI 回答。
     *
     * @param userId         用户 ID
     * @param question       用户问题
     * @param conversationId 对话 ID（为 null 时自动创建新对话）
     * @param callback       内容块回调，每收到一块内容时调用
     * @return 包含完整回答和对话 ID 的响应
     */
    public AdvisorResponse askStream(Long userId, String question, Long conversationId, Consumer<String> callback) {
        log.info("[AdvisorAppService] askStream: userId={}, convId={}, questionLen={}",
                userId, conversationId, question != null ? question.length() : 0);

        Long convId = ensureConversation(userId, question, conversationId);

        saveMessage(convId, ConversationRole.USER, question);

        String userPrompt = buildUserPrompt(userId, question);

        StringBuilder fullResponse = new StringBuilder();
        try {
            chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(content -> {
                        if (content != null && !content.isEmpty()) {
                            fullResponse.append(content);
                            callback.accept(content);
                        }
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("[AdvisorAppService] ChatClient stream failed: {}", e.getMessage(), e);
            throw new BizException(ResultCode.AI_GENERATION_FAILED, e.getMessage());
        }

        String answer = fullResponse.toString();
        if (answer.isBlank()) {
            log.warn("[AdvisorAppService] Empty streamed answer from ChatClient");
            throw new BizException(ResultCode.AI_GENERATION_FAILED, "empty response from AI");
        }

        saveMessage(convId, ConversationRole.ASSISTANT, answer);

        log.info("[AdvisorAppService] askStream completed: convId={}, answerLen={}", convId, answer.length());
        return AdvisorResponse.builder()
                .answer(answer)
                .conversationId(convId)
                .build();
    }

    /**
     * 确保对话存在：若 conversationId 为 null 则创建新对话。
     */
    private Long ensureConversation(Long userId, String question, Long conversationId) {
        if (conversationId != null) {
            return conversationId;
        }
        CreateConversationRequest req = new CreateConversationRequest();
        req.setTitle(buildTitle(question));
        ConversationVo conv = conversationAppService.create(req);
        log.info("[AdvisorAppService] Created new conversation: id={}, userId={}", conv.getId(), userId);
        return conv.getId();
    }

    /**
     * 保存消息到对话。
     */
    private void saveMessage(Long conversationId, ConversationRole role, String content) {
        AddMessageRequest req = AddMessageRequest.builder()
                .role(role)
                .content(content)
                .build();
        conversationAppService.addMessage(conversationId, req);
    }

    /**
     * 构建用户提示词，注入 RAG 检索到的旅行知识和用户记忆上下文。
     */
    private String buildUserPrompt(Long userId, String question) {
        String knowledgeContext = retrieveTravelKnowledge(question);
        String memoryContext = retrieveUserMemory(userId, question);

        StringBuilder sb = new StringBuilder();
        sb.append("User Question:\n").append(question).append("\n\n");

        if (knowledgeContext != null && !knowledgeContext.isBlank()) {
            sb.append("Travel Knowledge Context (from knowledge base):\n")
                    .append(knowledgeContext).append("\n\n");
        }

        if (memoryContext != null && !memoryContext.isBlank()) {
            sb.append(memoryContext).append("\n\n");
        }

        sb.append("Please answer the user's question based on the above context and your own knowledge. ");
        sb.append("Be accurate, helpful, and concise.");
        return sb.toString();
    }

    /**
     * 通过 RAG 检索与问题相关的旅行知识。
     * 检索失败时返回 null，不影响主流程。
     */
    private String retrieveTravelKnowledge(String question) {
        try {
            List<String> docs = ragService.search(question, RAG_TOP_K, null);
            if (docs == null || docs.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < docs.size(); i++) {
                sb.append(i + 1).append(". ").append(docs.get(i)).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("[AdvisorAppService] RAG knowledge retrieval failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过 RAG 检索与用户请求相关的偏好和旅行记忆。
     * 检索失败时返回 null，不影响主流程。
     */
    private String retrieveUserMemory(Long userId, String question) {
        try {
            return ragMemoryService.buildMemoryContext(userId, question);
        } catch (Exception e) {
            log.warn("[AdvisorAppService] RAG memory retrieval failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从用户问题中截取对话标题。
     */
    private String buildTitle(String question) {
        if (question == null || question.isBlank()) {
            return "Travel Advisor Chat";
        }
        String trimmed = question.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= TITLE_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, TITLE_MAX_LENGTH);
    }
}
