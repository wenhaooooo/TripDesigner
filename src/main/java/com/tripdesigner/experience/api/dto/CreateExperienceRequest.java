package com.tripdesigner.experience.api.dto;
/**
 * 创建体验请求 DTO。
 * 允许关联到具体行程/行程日/活动。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateExperienceRequest {
    @NotNull(message = "tripId is required")
    private Long tripId;

    private Long tripDayId;

    private Long tripActivityId;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    private String content;

    private Integer rating;

    private List<String> tags;

    private List<String> mediaUrls;
}
