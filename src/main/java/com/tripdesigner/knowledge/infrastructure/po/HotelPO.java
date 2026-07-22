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
 * 酒店持久化对象，映射至 kb_hotels 表。
 * amenities（设施列表）、roomInfo（房型信息）与 metadata 为 JSONB 字段。
 */
@Data
@TableName(value = "kb_hotels", autoResultMap = true)
public class HotelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cityId;

    private String name;

    private String nameLocal;

    private String category;

    private Integer starRating;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    /** JSONB：设施列表 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String amenities;

    /** JSONB：房型信息 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String roomInfo;

    private String contactInfo;

    private BigDecimal rating;

    private Integer reviewCount;

    private BigDecimal priceFrom;

    private String currencyCode;

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
