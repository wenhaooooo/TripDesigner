package com.tripdesigner.ai.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 服务 —— 封装 Embedding 和向量检索的通用能力。
 *
 * 提供 document 索引（向量化并存储）和相似度检索两个核心方法，
 * 供 RagMemoryService（方案1）和 DestinationKnowledgeService（方案2）复用。
 *
 * 向量存储使用 PostgreSQL pgvector，表结构见 V12 迁移脚本。
 * metadata 中通过 doc_type 区分文档类型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RAG_CACHE_PREFIX = "rag:search:";
    private static final Duration RAG_CACHE_TTL = Duration.ofHours(1);

    /**
     * 将文本内容向量化并存入向量库。
     *
     * @param content  文本内容
     * @param metadata 元数据（doc_type, user_id, destination 等）
     */
    public void index(String content, Map<String, Object> metadata) {
        try {
            Document doc = new Document(content, metadata);
            vectorStore.add(List.of(doc));
            log.debug("[RagService] Indexed document, metadata={}, contentLen={}", metadata, content.length());
        } catch (Exception e) {
            log.warn("[RagService] Failed to index document: {}", e.getMessage());
        }
    }

    /**
     * 语义检索：根据查询文本找到最相似的 Top-K 个文档。
     *
     * @param query    查询文本
     * @param topK     返回结果数
     * @param metadataFilterExpression 元数据过滤表达式（Spring AI FilterExpressionBuilder 语法）
     * @return 匹配的文档内容列表
     */
    public List<String> search(String query, int topK, String metadataFilterExpression) {
        // 尝试从缓存读取
        String cacheKey = RAG_CACHE_PREFIX + Integer.toHexString(Objects.hash(query, topK, metadataFilterExpression));
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("[RagService] Cache hit for query: {}", truncate(query, 80));
                return objectMapper.readValue(cached, new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.debug("[RagService] Cache read failed: {}", e.getMessage());
        }

        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.5);

            if (metadataFilterExpression != null && !metadataFilterExpression.isBlank()) {
                builder.filterExpression(metadataFilterExpression);
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());

            if (results == null || results.isEmpty()) {
                log.debug("[RagService] No results for query: {}", truncate(query, 80));
                return List.of();
            }

            log.debug("[RagService] Found {} results for query: {}", results.size(), truncate(query, 80));
            List<String> textResults = results.stream().map(Document::getText).toList();

            // 写入缓存
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(textResults), RAG_CACHE_TTL);
            } catch (Exception e) {
                log.debug("[RagService] Cache write failed: {}", e.getMessage());
            }

            return textResults;
        } catch (Exception e) {
            log.warn("[RagService] Search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 删除指定元数据条件下的所有文档。
     *
     * @param metadataFilterExpression 元数据过滤表达式
     */
    public void deleteByFilter(String metadataFilterExpression) {
        try {
            vectorStore.delete(metadataFilterExpression);
            log.debug("[RagService] Deleted documents by filter: {}", metadataFilterExpression);
        } catch (Exception e) {
            log.warn("[RagService] Failed to delete documents: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }
}
