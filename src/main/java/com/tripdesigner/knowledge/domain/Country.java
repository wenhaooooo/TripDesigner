package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 国家领域实体 —— 知识库聚合根之一。
 *
 * 代表一个国家/地区的静态知识，包含 ISO 代码、地理位置、货币、语言等。
 * languages 与 metadata 字段在持久层以 JSONB 存储，领域层使用原生 Java 类型。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
public class Country {

    /** 主键 ID */
    private Long id;

    /** 国家名称 */
    private String name;

    /** ISO 3166-1 alpha-2 代码，如 CN、US */
    private String isoCode2;

    /** ISO 3166-1 alpha-3 代码，如 CHN、USA */
    private String isoCode3;

    /** 所在洲 */
    private String continent;

    /** 首都 */
    private String capital;

    /** 货币代码（ISO 4217），如 CNY、USD */
    private String currencyCode;

    /** 官方语言列表 */
    private List<String> languages;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 附加元数据（JSONB） */
    private Map<String, Object> metadata;

    /** 数据来源标识，如 WIKIPEDIA、OPENSTREETMAP */
    private String source;

    /** 数据源内唯一 ID */
    private String sourceId;

    /** 内容哈希，用于增量同步去重 */
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
