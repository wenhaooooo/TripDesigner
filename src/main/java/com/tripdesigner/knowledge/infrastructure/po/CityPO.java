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
 * 城市持久化对象，映射至 kb_cities 表。
 * metadata 为 JSONB 字段。
 */
@Data
@TableName(value = "kb_cities", autoResultMap = true)
public class CityPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long countryId;

    private String name;

    private String nameLocal;

    private String timezone;

    private Integer population;

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
