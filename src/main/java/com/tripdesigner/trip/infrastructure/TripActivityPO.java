package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;

/**
 * 活动数据库持久化对象。
 * 映射至 trip_activities 表。
 */
@Data
@TableName("trip_activities")
public class TripActivityPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tripDayId;
    private String name;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private String category;
    private String place;
    private String notes;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}