package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 行程数据库持久化对象。
 * 映射至 trips 表。
 * status 字段使用 short 类型存储 TripStatus 的状态码。
 */
@Data
@TableName("trips")
public class TripPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String destinationName;
    private Short status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer budget;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}