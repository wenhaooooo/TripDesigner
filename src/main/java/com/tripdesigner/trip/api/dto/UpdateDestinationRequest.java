package com.tripdesigner.trip.api.dto;
/**
 * 更新目的地请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDestinationRequest {
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
