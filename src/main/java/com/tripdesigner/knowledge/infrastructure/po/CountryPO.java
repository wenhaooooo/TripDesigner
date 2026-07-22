package com.tripdesigner.knowledge.infrastructure.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 国家持久化对象，映射至 kb_countries 表。
 * languages 与 metadata 为 JSONB 字段，以 String 存储，通过 JsonbStringTypeHandler 进行 JDBC 转换。
 */
@Data
@TableName(value = "kb_countries", autoResultMap = true)
public class CountryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String isoCode2;

    private String isoCode3;

    private String continent;

    private String capital;

    private String currencyCode;

    /** JSONB：官方语言列表 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String languages;

    private BigDecimal latitude;

    private BigDecimal longitude;

    /** JSONB：附加元数据 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String metadata;

    private String source;

    private String sourceId;

    private String contentHash;

    private Instant lastSyncedAt;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
