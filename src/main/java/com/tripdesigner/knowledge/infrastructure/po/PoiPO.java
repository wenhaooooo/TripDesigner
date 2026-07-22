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
 * 景点（POI）持久化对象，映射至 kb_pois 表。
 * metadata 为 JSONB 字段。
 */
@Data
@TableName(value = "kb_pois", autoResultMap = true)
public class PoiPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cityId;

    private String name;

    private String nameLocal;

    private String category;

    private String subcategory;

    private String description;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String address;

    private String openingHours;

    private String priceInfo;

    private String contactInfo;

    private BigDecimal rating;

    private Integer reviewCount;

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
