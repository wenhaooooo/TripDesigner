package com.tripdesigner.ai.trip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateTripRequest {
    @NotBlank(message = "prompt cannot be blank")
    private String prompt;
}