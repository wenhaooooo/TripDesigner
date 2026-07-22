package com.tripdesigner.team.api.vo;

import com.tripdesigner.team.domain.ApplicationStatus;
import com.tripdesigner.team.domain.TeamApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TeamApplicationVo {
    private final Long id;
    private final Long teamId;
    private final Long applicantId;
    private final String applicantEmail;
    private final String message;
    private final String status;
    private final Instant processedAt;
    private final Instant createdAt;

    public static TeamApplicationVo from(TeamApplication a, String applicantEmail) {
        return TeamApplicationVo.builder()
                .id(a.getId())
                .teamId(a.getTeamId())
                .applicantId(a.getApplicantId())
                .applicantEmail(applicantEmail)
                .message(a.getMessage())
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .processedAt(a.getProcessedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
