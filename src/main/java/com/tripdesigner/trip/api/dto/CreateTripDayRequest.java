package com.tripdesigner.trip.api.dto;
/**
 * 创建行程日请求 DTO。
 */

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTripDayRequest {
    @NotNull(message = "dayNumber is required")
    private Integer dayNumber;

    private LocalDate date;

    @Size(max = 128)
    private String title;

    private String description;
}
