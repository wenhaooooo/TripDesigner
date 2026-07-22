package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 行程分享数据库持久化对象。
 * 映射至 trip_shares 表。
 * share_type 与 status 字段使用字符串存储枚举名。
 */
@Data
@TableName("trip_shares")
public class TripSharePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tripId;
    private Long ownerUserId;
    private String shareToken;
    private String shareType;
    private Integer maxViews;
    private Integer currentViews;
    private Instant expiresAt;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
