package com.tripdesigner.team.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 队伍申请领域实体。
 */
@Getter
@Builder
public class TeamApplication {

    private Long id;
    private Long teamId;
    private Long applicantId;
    private String message;
    private ApplicationStatus status;
    private Instant processedAt;
    private Long processedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static TeamApplication create(Long teamId, Long applicantId, String message) {
        return TeamApplication.builder()
                .teamId(teamId)
                .applicantId(applicantId)
                .message(message)
                .status(ApplicationStatus.PENDING)
                .version(0)
                .build();
    }

    public TeamApplication approve(Long processedBy) {
        return TeamApplication.builder()
                .id(id).teamId(teamId).applicantId(applicantId).message(message)
                .status(ApplicationStatus.APPROVED)
                .processedAt(Instant.now())
                .processedBy(processedBy)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public TeamApplication reject(Long processedBy) {
        return TeamApplication.builder()
                .id(id).teamId(teamId).applicantId(applicantId).message(message)
                .status(ApplicationStatus.REJECTED)
                .processedAt(Instant.now())
                .processedBy(processedBy)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public TeamApplication cancel() {
        return TeamApplication.builder()
                .id(id).teamId(teamId).applicantId(applicantId).message(message)
                .status(ApplicationStatus.CANCELLED)
                .processedAt(Instant.now())
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
