package com.tripdesigner.trip.api.dto;
/**
 * 更新行程请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTripRequest {
    @Size(max = 128)
    private String title;

    private String description;

    @Size(max = 64)
    private String destinationName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer budget;
}
