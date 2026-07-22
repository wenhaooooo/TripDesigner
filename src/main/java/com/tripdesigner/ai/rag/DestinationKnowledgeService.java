package com.tripdesigner.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 旅行目的地知识库服务（方案2）。
 *
 * 构建目的地知识库（景点、美食、交通、攻略等），Agent 生成行程时
 * 通过 RAG 检索实时信息，解决 LLM 幻觉问题，提供准确的目的地信息。
 *
 * 数据来源：
 * - 小红书帖子（XiaohongshuService 抓取的内容）
 * - 用户提供的目的地攻略
 * - 携程/马蜂窝等平台的结构化数据
 *
 * 向量存储 metadata:
 * - doc_type: DESTINATION_KNOWLEDGE
 * - destination: 目的地名称
 * - source: 数据来源（XIAOHONGSHU/MANUAL/CRAWLER）
 * - category: 内容类别（ATTRACTION/FOOD/TRANSPORT/TIPS）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestinationKnowledgeService {

    private final RagService ragService;

    private static final int DEFAULT_TOP_K = 5;

    /**
     * 索引目的地知识文档。
     *
     * @param destination 目的地名称
     * @param category    内容类别（ATTRACTION/FOOD/TRANSPORT/TIPS）
     * @param source      数据来源（XIAOHONGSHU/MANUAL/CRAWLER）
     * @param content     文档内容
     */
    public void indexKnowledge(String destination, String category, String source, String content) {
        Map<String, Object> metadata = Map.of(
                "doc_type", "DESTINATION_KNOWLEDGE",
                "destination", destination,
                "category", category,
                "source", source
        );
        ragService.index(content, metadata);
        log.info("[DestinationKnowledgeService] Indexed knowledge: dest={}, category={}, source={}",
                destination, category, source);
    }

    /**
     * 批量索引小红书帖子到目的地知识库。
     *
     * @param destination 目的地名称
     * @param posts       帖子内容列表
     */
    public void indexXiaohongshuPosts(String destination, List<String> posts) {
        for (String post : posts) {
            indexKnowledge(destination, "TIPS", "XIAOHONGSHU", post);
        }
        log.info("[DestinationKnowledgeService] Indexed {} posts for destination: {}",
                posts.size(), destination);
    }

    public void indexDestinationKnowledge(String destination, String category, String source, List<String> posts) {
        for (String post : posts) {
            indexKnowledge(destination, category, source, post);
        }
        log.info("[DestinationKnowledgeService] Indexed {} posts from {} for destination: {}",
                posts.size(), source, destination);
    }

    /**
     * 语义检索：根据用户请求找到最相关的目的地知识。
     *
     * @param destination 目的地名称
     * @param query       查询文本（用户请求）
     * @return 相关知识文档列表
     */
    public List<String> searchKnowledge(String destination, String query) {
        String filter = "doc_type == 'DESTINATION_KNOWLEDGE' && destination == '" + destination + "'";
        return ragService.search(query, DEFAULT_TOP_K, filter);
    }

    /**
     * 构建用于注入 Prompt 的目的地知识上下文。
     *
     * @param destination 目的地名称
     * @param userRequest 用户请求
     * @return RAG 检索后的知识摘要（若检索为空则返回 null）
     */
    public String buildKnowledgeContext(String destination, String userRequest) {
        if (destination == null || destination.isBlank()) {
            return null;
        }

        try {
            List<String> knowledge = searchKnowledge(destination, userRequest);

            if (knowledge.isEmpty()) {
                log.debug("[DestinationKnowledgeService] No knowledge found for: {}", destination);
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Destination Knowledge (RAG retrieved from knowledge base):\n");
            for (int i = 0; i < knowledge.size(); i++) {
                sb.append(i + 1).append(". ").append(knowledge.get(i)).append("\n");
            }

            log.info("[DestinationKnowledgeService] Built knowledge context for {}: {} docs",
                    destination, knowledge.size());
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("[DestinationKnowledgeService] Failed to build knowledge context: {}", e.getMessage());
            return null;
        }
    }

    public void deleteDestinationKnowledge() {
        ragService.deleteByFilter("doc_type == 'DESTINATION_KNOWLEDGE'");
        log.info("[DestinationKnowledgeService] Deleted all destination knowledge");
    }
}
