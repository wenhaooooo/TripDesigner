package com.tripdesigner.knowledge.pipeline;

import com.tripdesigner.ai.rag.RagService;
import com.tripdesigner.knowledge.chunker.Chunker;
import com.tripdesigner.knowledge.cleaner.DataCleaner;
import com.tripdesigner.knowledge.crawler.DataCrawler;
import com.tripdesigner.knowledge.domain.KnowledgeSource;
import com.tripdesigner.knowledge.parser.DataParser;
import com.tripdesigner.knowledge.resolver.EntityResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 知识库流水线编排器。
 *
 * <p>编排全球旅行知识库的完整数据处理流水线，将爬虫抓取的原始数据
 * 逐步处理为可检索的向量知识。
 *
 * <p>流水线阶段：
 * <pre>
 * Crawler → Parser → Cleaner → EntityResolver → Chunker → Embedding → VectorStorage
 *   ↓         ↓         ↓            ↓             ↓          ↓           ↓
 * 抓取数据  解析结构  清洗文本    关联实体ID     语义分块    向量化      存储向量
 * </pre>
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #processCrawlResult} — 处理爬虫返回的原始数据，走完整流水线</li>
 *   <li>{@link #processEntity} — 重新分块和向量化已有实体内容</li>
 * </ul>
 *
 * <p>向量化与存储使用 {@link RagService}（封装 Spring AI VectorStore + EmbeddingModel）。
 * 每个分块存储时携带元数据：doc_type、source、entity_type、entity_id、chunk_type、chunk_index。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePipeline {

    private final List<DataCrawler> crawlers;
    private final List<DataParser> parsers;
    private final DataCleaner cleaner;
    private final EntityResolver entityResolver;
    private final Chunker chunker;
    private final RagService ragService;

    /**
     * 处理爬虫返回的原始数据，走完整流水线：解析 → 清洗 → 实体解析 → 分块 → 向量化 → 存储。
     *
     * <p>处理流程：
     * <ol>
     *   <li>根据 sourceType 选择合适的 {@link DataParser} 解析原始数据</li>
     *   <li>用 {@link DataCleaner} 清洗文本内容</li>
     *   <li>用 {@link EntityResolver} 解析城市/国家实体 ID</li>
     *   <li>用 {@link Chunker} 进行语义分块</li>
     *   <li>对每个分块调用 {@link RagService#index} 向量化并存储</li>
     * </ol>
     *
     * @param source  知识源记录（含 sourceType、entityType、entityId 等）
     * @param rawData 爬虫返回的原始数据列表（每个 Map 为一条记录）
     * @return 处理的向量文档数量；处理失败返回 0
     */
    public int processCrawlResult(KnowledgeSource source, List<Map<String, Object>> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            log.debug("[KnowledgePipeline] No raw data to process for source: {}", source.getSourceType());
            return 0;
        }

        AtomicInteger totalIndexed = new AtomicInteger(0);
        String sourceType = source.getSourceType() != null ? source.getSourceType() : "UNKNOWN";

        for (Map<String, Object> item : rawData) {
            try {
                // 1. 提取原始内容
                String rawContent = extractRawContent(item);
                if (rawContent == null || rawContent.isBlank()) {
                    continue;
                }

                // 2. 解析原始内容
                DataParser parser = findParser(sourceType);
                Map<String, Object> parsed;
                if (parser != null) {
                    parsed = parser.parse(rawContent, sourceType);
                } else {
                    parsed = item;
                }

                // 3. 清洗文本
                String cleanedText = cleaner.clean(rawContent);
                if (cleanedText.isBlank()) {
                    continue;
                }

                // 4. 解析实体
                String title = extractTitle(item, parsed);
                Long entityId = resolveEntityId(item, parsed, source.getEntityId());

                // 5. 分块
                String entityType = source.getEntityType() != null ? source.getEntityType() : sourceType;
                List<Chunker.ChunkResult> chunks = chunker.chunk(cleanedText, title, entityType, entityId);

                // 6. 向量化并存储
                for (Chunker.ChunkResult chunk : chunks) {
                    Map<String, Object> metadata = buildMetadata(source, parsed, chunk, entityId);
                    ragService.index(chunk.content(), metadata);
                    totalIndexed.incrementAndGet();
                }

                log.debug("[KnowledgePipeline] Processed item: title={}, chunks={}",
                        title, chunks.size());
            } catch (Exception e) {
                log.warn("[KnowledgePipeline] Failed to process item from source {}: {}",
                        source.getSourceType(), e.getMessage());
            }
        }

        log.info("[KnowledgePipeline] Processed {} items from source {}, indexed {} chunks",
                rawData.size(), source.getSourceType(), totalIndexed.get());
        return totalIndexed.get();
    }

    /**
     * 重新分块和向量化已有实体内容。
     *
     * <p>用于实体内容更新后的重新索引。流程：清洗 → 分块 → 向量化 → 存储。
     *
     * @param entityType 实体类型（CITY、COUNTRY、POI）
     * @param entityId   实体 ID
     * @param content    实体内容文本
     * @return 处理的向量文档数量；处理失败返回 0
     */
    public int processEntity(String entityType, Long entityId, String content) {
        if (content == null || content.isBlank()) {
            log.debug("[KnowledgePipeline] Empty content for entity: {}/{}", entityType, entityId);
            return 0;
        }

        try {
            // 1. 清洗文本
            String cleanedText = cleaner.clean(content);
            if (cleanedText.isBlank()) {
                return 0;
            }

            // 2. 分块
            String title = entityType + ":" + entityId;
            List<Chunker.ChunkResult> chunks = chunker.chunk(cleanedText, title, entityType, entityId);

            // 3. 向量化并存储
            int indexed = 0;
            for (Chunker.ChunkResult chunk : chunks) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("doc_type", "KNOWLEDGE");
                metadata.put("entity_type", entityType);
                metadata.put("entity_id", entityId != null ? entityId : -1);
                metadata.put("chunk_type", chunk.chunkType());
                metadata.put("chunk_index", chunk.chunkIndex());
                metadata.put("source", "ENTITY_UPDATE");

                ragService.index(chunk.content(), metadata);
                indexed++;
            }

            log.info("[KnowledgePipeline] Re-indexed entity {}/{}: {} chunks",
                    entityType, entityId, indexed);
            return indexed;
        } catch (Exception e) {
            log.warn("[KnowledgePipeline] Failed to process entity {}/{}: {}",
                    entityType, entityId, e.getMessage());
            return 0;
        }
    }

    /**
     * 根据数据源类型查找支持的解析器。
     *
     * @param sourceType 数据源类型
     * @return 支持的解析器；未找到返回 null
     */
    private DataParser findParser(String sourceType) {
        return parsers.stream()
                .filter(p -> p.supports(sourceType))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从原始数据项中提取文本内容。
     */
    private String extractRawContent(Map<String, Object> item) {
        Object content = item.getOrDefault("wikitext",
                item.getOrDefault("fullText",
                        item.getOrDefault("summary",
                                item.getOrDefault("content",
                                        item.getOrDefault("description", null)))));

        if (content instanceof String s && !s.isBlank()) {
            return s;
        }

        if (!item.isEmpty()) {
            return item.toString();
        }

        return null;
    }

    /**
     * 提取标题。
     */
    private String extractTitle(Map<String, Object> rawData, Map<String, Object> parsed) {
        Object title = parsed.getOrDefault("title", rawData.get("title"));
        if (title instanceof String s && !s.isBlank()) {
            return s;
        }
        Object name = rawData.get("name");
        if (name instanceof String s && !s.isBlank()) {
            return s;
        }
        return "Unknown";
    }

    /**
     * 解析实体 ID（城市或国家）。
     *
     * @param rawData        原始数据 Map
     * @param parsed         解析后的数据 Map
     * @param existingEntityId 已关联的实体 ID（来自 KnowledgeSource）
     * @return 实体 ID；未解析到时返回 existingEntityId 或 null
     */
    private Long resolveEntityId(Map<String, Object> rawData, Map<String, Object> parsed, Long existingEntityId) {
        // 如果 KnowledgeSource 已关联实体 ID，直接使用
        if (existingEntityId != null) {
            return existingEntityId;
        }

        try {
            String name = extractTitle(rawData, parsed);
            Object countryObj = rawData.getOrDefault("country",
                    parsed.getOrDefault("country", null));
            String country = countryObj instanceof String s ? s : null;

            // 尝试解析为城市
            Long cityId = entityResolver.resolveCity(name, country);
            if (cityId != null) {
                return cityId;
            }

            // 尝试解析为国家
            Long countryId = entityResolver.resolveCountry(name);
            if (countryId != null) {
                return countryId;
            }

            return null;
        } catch (Exception e) {
            log.debug("[KnowledgePipeline] Entity resolution failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建向量存储元数据。
     */
    private Map<String, Object> buildMetadata(KnowledgeSource source, Map<String, Object> parsed,
                                               Chunker.ChunkResult chunk, Long entityId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("doc_type", "KNOWLEDGE");
        metadata.put("source", source.getSourceType());
        metadata.put("entity_type", source.getEntityType() != null ? source.getEntityType() : source.getSourceType());
        metadata.put("entity_id", entityId != null ? entityId : -1);
        metadata.put("chunk_type", chunk.chunkType());
        metadata.put("chunk_index", chunk.chunkIndex());
        metadata.put("title", chunk.title());

        // 合并解析后的元数据（分类、坐标等）
        Map<String, Object> cleanedParsed = cleaner.cleanMetadata(parsed);
        if (cleanedParsed.containsKey("categories")) {
            metadata.put("categories", cleanedParsed.get("categories"));
        }
        if (cleanedParsed.containsKey("latitude")) {
            metadata.put("latitude", cleanedParsed.get("latitude"));
        }
        if (cleanedParsed.containsKey("longitude")) {
            metadata.put("longitude", cleanedParsed.get("longitude"));
        }

        return metadata;
    }
}
