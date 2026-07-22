package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.Instant;

/**
 * 知识数据源领域实体 —— 记录知识抓取的来源与状态。
 *
 * 跟踪每个数据源的抓取状态（PENDING/SUCCESS/FAILED）、ETag、重试次数等，
 * 支持增量同步与失败重试。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
@With
public class KnowledgeSource {

    /** 主键 ID */
    private Long id;

    /** 来源类型，如 WIKIPEDIA、CTRIP、MAFENGWO */
    private String sourceType;

    /** 来源 URL */
    private String sourceUrl;

    /** 数据源内唯一 ID */
    private String sourceId;

    /** 关联实体类型 */
    private String entityType;

    /** 关联实体 ID */
    private Long entityId;

    /** 原始抓取内容 */
    private String rawContent;

    /** 内容哈希 */
    private String contentHash;

    /** 抓取时间 */
    private Instant fetchedAt;

    /** HTTP ETag，用于增量同步 */
    private String etag;

    /** HTTP Last-Modified 头 */
    private String lastModified;

    /** 状态：PENDING / SUCCESS / FAILED */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;
}
