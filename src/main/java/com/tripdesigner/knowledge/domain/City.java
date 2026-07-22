package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 城市领域实体 —— 知识库聚合根之一。
 *
 * 隶属于某个国家（countryId），包含时区、人口、坐标等地理信息。
 * metadata 字段在持久层以 JSONB 存储。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
public class City {

    /** 主键 ID */
    private Long id;

    /** 所属国家 ID */
    private Long countryId;

    /** 城市名称（英文/通用） */
    private String name;

    /** 当地语言名称 */
    private String nameLocal;

    /** 时区标识，如 Asia/Shanghai */
    private String timezone;

    /** 人口数量 */
    private Integer population;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

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
