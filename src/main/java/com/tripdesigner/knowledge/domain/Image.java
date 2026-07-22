package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 图片领域实体 —— 知识库实体的关联图片。
 *
 * 通过 entityType + entityId 多态关联到任意知识实体（国家、城市、景点等）。
 * metadata 字段在持久层以 JSONB 存储。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
public class Image {

    /** 主键 ID */
    private Long id;

    /** 关联实体类型 */
    private String entityType;

    /** 关联实体 ID */
    private Long entityId;

    /** 原图 URL */
    private String url;

    /** 缩略图 URL */
    private String urlThumb;

    /** 图片说明 */
    private String caption;

    /** 无障碍替代文本 */
    private String altText;

    /** 图片宽度（像素） */
    private Integer width;

    /** 图片高度（像素） */
    private Integer height;

    /** 附加元数据（JSONB） */
    private Map<String, Object> metadata;

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
