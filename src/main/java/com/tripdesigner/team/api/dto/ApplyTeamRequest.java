package com.tripdesigner.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyTeamRequest {
    @Size(max = 500)
    private String message;
}
