package com.tripdesigner.experience.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 体验数据库持久化对象。
 * 映射至 trip_experiences 表。
 * tags 和 mediaUrls 字段存储 JSON 数组字符串。
 */
@Data
@TableName("trip_experiences")
public class TripExperiencePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tripId;
    private Long tripDayId;
    private Long tripActivityId;
    private String title;
    private String content;
    private Integer rating;
    private String tags;         // JSON: ["tag1","tag2"]
    private String mediaUrls;    // JSON: ["url1","url2"]
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;
}
