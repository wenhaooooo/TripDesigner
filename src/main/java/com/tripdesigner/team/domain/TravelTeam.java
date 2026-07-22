package com.tripdesigner.team.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 旅行组队领域实体（聚合根）。
 *
 * 包含目的地、行程日期、队伍类型、成员上限等核心字段。
 * 不可变模式，状态变更通过 withXxx 返回新实例。
 */
@Getter
@Builder
public class TravelTeam {

    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private TeamType teamType;
    private List<String> interests;
    private Integer maxMembers;
    private Integer currentMembers;
    private String genderRequirement;
    private Integer minAge;
    private Integer maxAge;
    private String contact;
    private TeamStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static TravelTeam create(Long creatorId, String title, String description, String destination,
                                      LocalDate startDate, LocalDate endDate, TeamType teamType,
                                      List<String> interests, Integer maxMembers, String genderRequirement,
                                      Integer minAge, Integer maxAge, String contact) {
        return TravelTeam.builder()
                .creatorId(creatorId)
                .title(title)
                .description(description)
                .destination(destination)
                .startDate(startDate)
                .endDate(endDate)
                .teamType(teamType)
                .interests(interests != null ? interests : List.of())
                .maxMembers(maxMembers != null ? maxMembers : 4)
                .currentMembers(1)
                .genderRequirement(genderRequirement != null ? genderRequirement : "ANY")
                .minAge(minAge)
                .maxAge(maxAge)
                .contact(contact)
                .status(TeamStatus.OPEN)
                .version(0)
                .build();
    }

    public TravelTeam withStatus(TeamStatus newStatus) {
        return TravelTeam.builder()
                .id(id).creatorId(creatorId).title(title).description(description)
                .destination(destination).startDate(startDate).endDate(endDate).teamType(teamType)
                .interests(interests).maxMembers(maxMembers).currentMembers(currentMembers)
                .genderRequirement(genderRequirement).minAge(minAge).maxAge(maxAge).contact(contact)
                .status(newStatus).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public TravelTeam withMemberCount(int delta) {
        return TravelTeam.builder()
                .id(id).creatorId(creatorId).title(title).description(description)
                .destination(destination).startDate(startDate).endDate(endDate).teamType(teamType)
                .interests(interests).maxMembers(maxMembers)
                .currentMembers(Math.max(1, (currentMembers != null ? currentMembers : 1) + delta))
                .genderRequirement(genderRequirement).minAge(minAge).maxAge(maxAge).contact(contact)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public boolean isFull() {
        return currentMembers != null && maxMembers != null && currentMembers >= maxMembers;
    }

    public boolean isOpenForApplication() {
        return status == TeamStatus.OPEN && !isFull();
    }
}
