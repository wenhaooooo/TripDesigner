package com.tripdesigner.trip.api.dto;
/**
 * 创建行程请求 DTO。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTripRequest {
    @NotBlank(message = "title is required")
    @Size(max = 128)
    private String title;

    private String description;

    @Size(max = 64)
    private String destinationName;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    private Integer budget;
}
