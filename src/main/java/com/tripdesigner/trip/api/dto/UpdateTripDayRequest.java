package com.tripdesigner.trip.api.dto;
/**
 * 更新行程日请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTripDayRequest {
    private LocalDate date;

    @Size(max = 128)
    private String title;

    private String description;
}
