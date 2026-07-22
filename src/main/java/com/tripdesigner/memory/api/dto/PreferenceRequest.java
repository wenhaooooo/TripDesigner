package com.tripdesigner.memory.api.dto;
/**
 * 保存偏好请求 DTO。
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreferenceRequest {
    @NotBlank(message = "category is required")
    @Size(max = 50, message = "category must be at most 50 characters")
    private String category;

    @NotNull(message = "preference is required")
    private Map<String, Object> preference;

    private String source = "MANUAL";
}
