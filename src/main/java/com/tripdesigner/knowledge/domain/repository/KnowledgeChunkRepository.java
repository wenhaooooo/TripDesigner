package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.KnowledgeChunk;

import java.util.List;
import java.util.Optional;

/**
 * 知识分块仓储接口。
 *
 * 定义知识分块的持久化与向量检索操作。
 * 实现在 infrastructure 层（KnowledgeChunkRepositoryImpl），
 * 向量检索通过 pgvector 的余弦距离算子（&lt;=&gt;）实现。
 */
public interface KnowledgeChunkRepository {

    /** 按主键查询 */
    Optional<KnowledgeChunk> findById(Long id);

    /** 按实体类型与实体 ID 查询分块列表 */
    List<KnowledgeChunk> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /** 保存（新增或更新） */
    KnowledgeChunk save(KnowledgeChunk chunk);

    /** 批量保存 */
    List<KnowledgeChunk> saveAll(List<KnowledgeChunk> chunks);

    /**
     * 按实体类型过滤的向量相似度检索。
     *
     * @param queryVector 查询向量
     * @param entityType  实体类型
     * @param topK        返回条数
     * @return 按相似度降序排列的分块列表
     */
    List<KnowledgeChunk> vectorSearch(float[] queryVector, String entityType, int topK);

    /**
     * 全库向量相似度检索。
     *
     * @param queryVector 查询向量
     * @param topK        返回条数
     * @return 按相似度降序排列的分块列表
     */
    List<KnowledgeChunk> vectorSearchAll(float[] queryVector, int topK);

    /** 按实体删除关联的所有分块 */
    void deleteByEntity(String entityType, Long entityId);
}
