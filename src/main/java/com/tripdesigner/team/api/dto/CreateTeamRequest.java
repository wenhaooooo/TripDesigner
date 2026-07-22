package com.tripdesigner.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateTeamRequest {
    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "destination is required")
    @Size(max = 100)
    private String destination;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    /** 队伍类型：BUDDY/CARPOOL/ROOM_SHARE/GROUP，默认 BUDDY */
    private String teamType = "BUDDY";

    private List<String> interests;

    private Integer maxMembers = 4;

    /** 性别要求：ANY/MALE/FEMALE */
    private String genderRequirement = "ANY";

    private Integer minAge;
    private Integer maxAge;

    private String contact;
}
