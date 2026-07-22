package com.tripdesigner.checkin.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateCheckinRequest {
    @NotNull(message = "tripId is required")
    private Long tripId;

    private Long tripDayId;

    private Long activityId;

    @Size(max = 255)
    private String placeName;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 2000)
    private String notes;

    private List<String> photoUrls;
}
