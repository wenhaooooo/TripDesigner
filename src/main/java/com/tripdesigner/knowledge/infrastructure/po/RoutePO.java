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
 * 路线持久化对象，映射至 kb_routes 表。
 * transportationMethods（交通方式列表）与 metadata 为 JSONB 字段。
 */
@Data
@TableName(value = "kb_routes", autoResultMap = true)
public class RoutePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromCityId;

    private Long toCityId;

    private String routeType;

    private BigDecimal distanceKm;

    private String estimatedDuration;

    /** JSONB：交通方式列表 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String transportationMethods;

    private String description;

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
