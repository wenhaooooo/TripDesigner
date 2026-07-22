package com.tripdesigner.knowledge.chunker;

import java.util.List;

/**
 * 语义分块器接口。
 *
 * 将长文本按语义边界切分为适合向量化的分块（chunk），
 * 每个分块包含独立的标题、内容和预估 token 数。
 *
 * <p>分块策略由实现类决定：
 * <ul>
 *   <li>{@link SemanticChunker} — 按章节标题切分，超大章节再按段落切分</li>
 * </ul>
 *
 * <p>分块结果用于 RAG 向量索引，每个分块对应向量库中的一条文档记录。
 */
public interface Chunker {

    /**
     * 将内容切分为语义分块。
     *
     * @param content    待分块的文本内容
     * @param title      整体标题（如页面标题）
     * @param entityType 实体类型（CITY、COUNTRY、POI 等）
     * @param entityId   实体 ID，用于元数据关联
     * @return 分块结果列表，按 chunkIndex 顺序排列
     */
    List<ChunkResult> chunk(String content, String title, String entityType, Long entityId);

    /**
     * 获取单个分块的最大 token 数。
     *
     * @return 最大 token 数
     */
    int getMaxTokens();

    /**
     * 分块结果记录。
     *
     * @param chunkType  分块类型（通常为章节名称，如 "Introduction"、"See"、"Eat"）
     * @param chunkIndex 分块序号（从 0 开始）
     * @param title      分块标题
     * @param content    分块文本内容
     * @param tokenCount 预估 token 数（tokens ≈ characters / 4）
     */
    record ChunkResult(
            String chunkType,
            int chunkIndex,
            String title,
            String content,
            int tokenCount
    ) {}
}
