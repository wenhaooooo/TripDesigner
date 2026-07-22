package com.tripdesigner.team.domain;

import java.util.List;
import java.util.Optional;

public interface TeamApplicationRepository {
    TeamApplication save(TeamApplication application);
    Optional<TeamApplication> findById(Long id);
    List<TeamApplication> findByTeamId(Long teamId);
    List<TeamApplication> findByApplicant(Long applicantId);
    Optional<TeamApplication> findByTeamAndApplicant(Long teamId, Long applicantId);
    void deleteById(Long id);
}
