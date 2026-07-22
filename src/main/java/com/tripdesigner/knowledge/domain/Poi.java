package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 景点（POI）领域实体 —— 知识库聚合根之一。
 *
 * Point of Interest，隶属于某个城市（cityId），包含分类、坐标、营业时间、评分等。
 * metadata 字段在持久层以 JSONB 存储。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
public class Poi {

    /** 主键 ID */
    private Long id;

    /** 所属城市 ID */
    private Long cityId;

    /** 景点名称 */
    private String name;

    /** 当地语言名称 */
    private String nameLocal;

    /** 主分类，如 sightseeing、museum、park */
    private String category;

    /** 子分类 */
    private String subcategory;

    /** 描述 */
    private String description;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 地址 */
    private String address;

    /** 营业时间 */
    private String openingHours;

    /** 价格信息 */
    private String priceInfo;

    /** 联系方式 */
    private String contactInfo;

    /** 评分（0-5） */
    private BigDecimal rating;

    /** 评论数量 */
    private Integer reviewCount;

    /** 附加元数据（JSONB） */
    private Map<String, Object> metadata;

    /** 数据来源标识 */
    private String source;

    /** 数据源内唯一 ID */
    private String sourceId;

    /** 内容哈希 */
    private String contentHash;

    /** 最后同步时间 */
    private Instant lastSyncedAt;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;
}
