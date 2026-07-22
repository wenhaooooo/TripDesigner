package com.tripdesigner.experience.api.vo;
/**
 * 体验视图对象（VO）。
 */

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ExperienceVo {
    private Long id;
    private Long tripId;
    private Long tripDayId;
    private Long tripActivityId;
    private String title;
    private String content;
    private Integer rating;
    private List<String> tags;
    private List<String> mediaUrls;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
