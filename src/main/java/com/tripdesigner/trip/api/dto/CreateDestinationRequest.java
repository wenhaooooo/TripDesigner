package com.tripdesigner.trip.api.dto;
/**
 * 创建目的地请求 DTO。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDestinationRequest {
    @NotBlank(message = "name is required")
    @Size(max = 128)
    private String name;

    @Size(max = 64)
    private String country;

    @Size(max = 64)
    private String region;

    @Size(max = 32)
    private String category;

    private String description;
}
