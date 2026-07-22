package com.tripdesigner.team.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 队伍成员领域实体。
 */
@Getter
@Builder
public class TeamMember {

    private Long id;
    private Long teamId;
    private Long userId;
    private MemberRole role;
    private Instant joinedAt;

    public static TeamMember creator(Long teamId, Long userId) {
        return TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .role(MemberRole.CREATOR)
                .joinedAt(Instant.now())
                .build();
    }

    public static TeamMember member(Long teamId, Long userId) {
        return TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .role(MemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
    }
}
