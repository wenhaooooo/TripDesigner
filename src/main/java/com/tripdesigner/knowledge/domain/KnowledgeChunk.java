package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 知识分块领域实体 —— RAG 检索的最小单元。
 *
 * 将知识库实体（国家、城市、景点等）的内容切分为多个 chunk，
 * 每个 chunk 生成向量嵌入（embedding）用于语义检索。
 * embedding 字段不在此实体中管理，由持久层通过自定义 SQL 处理。
 * metadata 字段在持久层以 JSONB 存储。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
public class KnowledgeChunk {

    /** 主键 ID */
    private Long id;

    /** 关联实体类型，如 COUNTRY、CITY、POI */
    private String entityType;

    /** 关联实体 ID */
    private Long entityId;

    /** 分块类型，如 OVERVIEW、SECTION、QA */
    private String chunkType;

    /** 分块序号 */
    private Integer chunkIndex;

    /** 标题 */
    private String title;

    /** 文本内容 */
    private String content;

    /** 内容哈希 */
    private String contentHash;

    /** 附加元数据（JSONB） */
    private Map<String, Object> metadata;

    /** 语言代码，如 zh、en */
    private String language;

    /** Token 数量 */
    private Integer tokenCount;

    /** 数据来源标识 */
    private String source;

    /** 数据源内唯一 ID */
    private String sourceId;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;
}
