package com.tripdesigner.trip.api.dto;
/**
 * 更新活动请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateTripActivityRequest {
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

    private Integer sortOrder;
}
