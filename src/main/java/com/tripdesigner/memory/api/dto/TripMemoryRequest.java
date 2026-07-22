package com.tripdesigner.memory.api.dto;
/**
 * 保存旅行记忆请求 DTO。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TripMemoryRequest {
    @NotNull(message = "tripId is required")
    private Long tripId;

    @NotBlank(message = "memoryType is required")
    @Size(max = 50, message = "memoryType must be at most 50 characters")
    private String memoryType;

    @NotBlank(message = "content is required")
    private String content;

    private List<String> tags;
}
