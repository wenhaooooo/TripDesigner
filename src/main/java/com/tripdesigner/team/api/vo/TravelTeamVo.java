package com.tripdesigner.team.api.vo;

import com.tripdesigner.team.domain.TeamStatus;
import com.tripdesigner.team.domain.TeamType;
import com.tripdesigner.team.domain.TravelTeam;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TravelTeamVo {
    private final Long id;
    private final Long creatorId;
    private final String creatorEmail;
    private final String title;
    private final String description;
    private final String destination;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String teamType;
    private final List<String> interests;
    private final Integer maxMembers;
    private final Integer currentMembers;
    private final String genderRequirement;
    private final Integer minAge;
    private final Integer maxAge;
    private final String status;
    private final Instant createdAt;
    private final boolean isCreator;
    private final boolean isMember;
    private final boolean hasApplied;

    public static TravelTeamVo from(TravelTeam t, String creatorEmail, boolean isCreator, boolean isMember, boolean hasApplied) {
        return TravelTeamVo.builder()
                .id(t.getId())
                .creatorId(t.getCreatorId())
                .creatorEmail(creatorEmail)
                .title(t.getTitle())
                .description(t.getDescription())
                .destination(t.getDestination())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .teamType(t.getTeamType() != null ? t.getTeamType().name() : null)
                .interests(t.getInterests())
                .maxMembers(t.getMaxMembers())
                .currentMembers(t.getCurrentMembers())
                .genderRequirement(t.getGenderRequirement())
                .minAge(t.getMinAge())
                .maxAge(t.getMaxAge())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .createdAt(t.getCreatedAt())
                .isCreator(isCreator)
                .isMember(isMember)
                .hasApplied(hasApplied)
                .build();
    }
}
