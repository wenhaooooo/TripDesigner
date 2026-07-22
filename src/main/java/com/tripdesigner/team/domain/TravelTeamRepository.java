package com.tripdesigner.team.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelTeamRepository {
    TravelTeam save(TravelTeam team);
    Optional<TravelTeam> findById(Long id);
    List<TravelTeam> findOpen(int page, int size);
    List<TravelTeam> findByCreator(Long userId);
    List<TravelTeam> findByDestination(String destination, int page, int size);
    /** 按目的地+日期范围匹配队伍（日期重叠） */
    List<TravelTeam> findMatches(String destination, LocalDate startDate, LocalDate endDate);
    void deleteById(Long id);
}
