package com.tripdesigner.trip.api.dto;
/**
 * 创建活动请求 DTO。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateTripActivityRequest {
    @NotBlank(message = "name is required")
    @Size(max = 128)
    private String name;

    private String description;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 32)
    private String category;

    @Size(max = 128)
    private String place;

    private String notes;
}
