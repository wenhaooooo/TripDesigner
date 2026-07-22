package com.tripdesigner.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户记忆 RAG 服务（方案1）。
 *
 * 将用户偏好和旅行记忆向量化存储，在生成行程时通过语义检索
 * 找到与当前请求最相关的记忆，只注入相关内容到 Prompt，
 * 避免全量记忆导致 Prompt 过长和 LLM 注意力分散。
 *
 * 对比改进：
 * - 旧：MemoryAppService.getPreferenceSummary() 返回全量偏好文本
 * - 新：检索与用户请求语义相关的 Top-K 条记忆
 *
 * 向量存储 metadata:
 * - doc_type: USER_PREFERENCE / USER_MEMORY
 * - user_id: 用户 ID
 * - category: 偏好类别（仅 USER_PREFERENCE）
 * - memory_type: 记忆类型（仅 USER_MEMORY）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagMemoryService {

    private final RagService ragService;

    private static final int DEFAULT_TOP_K = 5;

    /**
     * 索引用户偏好：将单条偏好向量化存入向量库。
     *
     * @param userId   用户 ID
     * @param category 偏好类别
     * @param content  偏好内容（如 "预算=经济型, 住宿=民宿"）
     */
    public void indexPreference(Long userId, String category, String content) {
        Map<String, Object> metadata = Map.of(
                "doc_type", "USER_PREFERENCE",
                "user_id", userId.toString(),
                "category", category
        );
        ragService.index(content, metadata);
    }

    /**
     * 索引用户旅行记忆：将单条记忆向量化存入向量库。
     *
     * @param userId     用户 ID
     * @param memoryType 记忆类型（HIGHLIGHT/LESSON_LEARNED/ADVICE）
     * @param content    记忆内容
     */
    public void indexTripMemory(Long userId, String memoryType, String content) {
        Map<String, Object> metadata = Map.of(
                "doc_type", "USER_MEMORY",
                "user_id", userId.toString(),
                "memory_type", memoryType
        );
        ragService.index(content, metadata);
    }

    /**
     * 语义检索：根据用户请求找到最相关的偏好。
     *
     * @param userId      用户 ID
     * @param userRequest 用户当前请求
     * @return 相关偏好列表
     */
    public List<String> searchRelevantPreferences(Long userId, String userRequest) {
        String filter = "user_id == '" + userId + "' && doc_type == 'USER_PREFERENCE'";
        return ragService.search(userRequest, DEFAULT_TOP_K, filter);
    }

    /**
     * 语义检索：根据用户请求找到最相关的旅行记忆。
     *
     * @param userId      用户 ID
     * @param userRequest 用户当前请求
     * @return 相关记忆列表
     */
    public List<String> searchRelevantMemories(Long userId, String userRequest) {
        String filter = "user_id == '" + userId + "' && doc_type == 'USER_MEMORY'";
        return ragService.search(userRequest, DEFAULT_TOP_K, filter);
    }

    /**
     * 构建用于注入 Prompt 的记忆摘要。
     * 检索与用户请求相关的偏好和记忆，拼成自然语言。
     *
     * @param userId      用户 ID
     * @param userRequest 用户请求
     * @return RAG 检索后的记忆摘要（若检索为空则返回 fallback 文本）
     */
    public String buildMemoryContext(Long userId, String userRequest) {
        try {
            List<String> preferences = searchRelevantPreferences(userId, userRequest);
            List<String> memories = searchRelevantMemories(userId, userRequest);

            if (preferences.isEmpty() && memories.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            if (!preferences.isEmpty()) {
                sb.append("Relevant User Preferences (RAG retrieved):\n");
                for (String p : preferences) {
                    sb.append("- ").append(p).append("\n");
                }
            }

            if (!memories.isEmpty()) {
                sb.append("\nRelevant Trip Memories (RAG retrieved):\n");
                for (String m : memories) {
                    sb.append("- ").append(m).append("\n");
                }
            }

            log.info("[RagMemoryService] Built memory context for user {}, prefs={}, memories={}",
                    userId, preferences.size(), memories.size());
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("[RagMemoryService] Failed to build memory context: {}", e.getMessage());
            return null;
        }
    }
}
