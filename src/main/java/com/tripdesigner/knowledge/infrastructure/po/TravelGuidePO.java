package com.tripdesigner.knowledge.infrastructure.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

/**
 * 旅行指南持久化对象，映射至 kb_travel_guides 表。
 * sections（分节结构）与 metadata 为 JSONB 字段。
 */
@Data
@TableName(value = "kb_travel_guides", autoResultMap = true)
public class TravelGuidePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cityId;

    private Long countryId;

    private String title;

    private String language;

    private String content;

    private String summary;

    /** JSONB：分节结构 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String sections;

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
