package com.tripdesigner.behavior.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class TrackBehaviorRequest {
    @NotBlank(message = "behaviorType is required")
    @Size(max = 30)
    private String behaviorType;

    @NotBlank(message = "targetType is required")
    @Size(max = 30)
    private String targetType;

    private Long targetId;

    private Map<String, Object> context;
}
