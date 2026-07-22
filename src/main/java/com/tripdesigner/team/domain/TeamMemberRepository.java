package com.tripdesigner.team.domain;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository {
    TeamMember save(TeamMember member);
    Optional<TeamMember> findByTeamAndUser(Long teamId, Long userId);
    List<TeamMember> findByTeamId(Long teamId);
    List<TeamMember> findByUserId(Long userId);
    void deleteByTeamIdAndUserId(Long teamId, Long userId);
    int countByTeamId(Long teamId);
}
