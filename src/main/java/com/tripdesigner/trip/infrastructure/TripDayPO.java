package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 行程日数据库持久化对象。
 * 映射至 trip_days 表。
 */
@Data
@TableName("trip_days")
public class TripDayPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tripId;
    private Integer dayNumber;
    private LocalDate date;
    private String title;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}