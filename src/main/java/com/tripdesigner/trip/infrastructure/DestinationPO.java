package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 目的地数据库持久化对象。
 * 映射至 destinations 表。
 */
@Data
@TableName("destinations")
public class DestinationPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String country;
    private String region;
    private String category;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}