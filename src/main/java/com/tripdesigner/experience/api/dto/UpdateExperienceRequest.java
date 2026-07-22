package com.tripdesigner.experience.api.dto;
/**
 * 更新体验请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateExperienceRequest {
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    private String content;

    private Integer rating;

    private List<String> tags;

    private List<String> mediaUrls;
}
