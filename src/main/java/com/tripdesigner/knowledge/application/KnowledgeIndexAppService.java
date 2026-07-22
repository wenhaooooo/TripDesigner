package com.tripdesigner.knowledge.application;

import com.tripdesigner.knowledge.domain.KnowledgeChunk;
import com.tripdesigner.knowledge.domain.repository.KnowledgeChunkRepository;
import com.tripdesigner.knowledge.pipeline.KnowledgePipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库索引应用服务。
 *
 * 负责将文本内容转换为可检索的向量索引，核心流程委托给 {@link KnowledgePipeline}：
 * <ol>
 *   <li>分块（Chunking）：将长文本切分为适当大小的知识块</li>
 *   <li>向量化（Embedding）：将每个知识块转换为向量</li>
 *   <li>存储（Storage）：将知识块及其向量持久化</li>
 * </ol>
 *
 * 该服务是知识库写入路径的核心，被 {@link CrawlAppService} 和重建索引流程调用。
 * 直接的知识块操作通过 {@link KnowledgeChunkRepository} 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexAppService {

    private final KnowledgePipeline knowledgePipeline;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    /**
     * 对指定实体的文本内容进行分块、向量化并存储。
     *
     * 委托给 {@link KnowledgePipeline#processEntity} 执行完整的索引流程：
     * 清洗 → 分块 → 向量化 → 向量存储。
     *
     * @param entityType 实体类型：COUNTRY, CITY, POI, RESTAURANT, HOTEL, TRAVEL_GUIDE, ROUTE
     * @param entityId   实体 ID
     * @param content    待索引的文本内容
     * @return 已索引的知识块数量
     */
    @Transactional
    public int indexEntity(String entityType, Long entityId, String content) {
        if (content == null || content.isBlank()) {
            log.debug("[KnowledgeIndexAppService] Skip indexing empty content: entityType={}, entityId={}",
                    entityType, entityId);
            return 0;
        }

        int indexed = knowledgePipeline.processEntity(entityType, entityId, content);
        log.info("[KnowledgeIndexAppService] Indexed {} chunks for {}:{}",
                indexed, entityType, entityId);
        return indexed;
    }

    /**
     * 存储单个知识块。
     *
     * 注意：{@link KnowledgeChunk} 领域实体不包含 embedding 字段，
     * 向量数据由持久层通过自定义 SQL 单独处理。
     * 本方法仅保存知识块的结构化数据（内容、元数据、关联实体等）。
     *
     * @param chunk 知识块实体（包含内容、元数据等）
     */
    @Transactional
    public void indexKnowledgeChunk(KnowledgeChunk chunk) {
        knowledgeChunkRepository.save(chunk);
        log.debug("[KnowledgeIndexAppService] Saved chunk: entityType={}, entityId={}, chunkIndex={}",
                chunk.getEntityType(), chunk.getEntityId(), chunk.getChunkIndex());
    }

    /**
     * 删除指定实体的所有知识块。
     * 用于重建索引前的清理操作。
     *
     * @param entityType 实体类型
     * @param entityId   实体 ID
     */
    @Transactional
    public void deleteChunks(String entityType, Long entityId) {
        knowledgeChunkRepository.deleteByEntity(entityType, entityId);
        log.debug("[KnowledgeIndexAppService] Deleted chunks for {}:{}",
                entityType, entityId);
    }

    /**
     * 查询指定实体的所有知识块。
     *
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @return 知识块列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeChunk> findChunks(String entityType, Long entityId) {
        return knowledgeChunkRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
