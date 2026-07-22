package com.tripdesigner.experience.domain;

import java.util.List;
import java.util.Optional;

/**
 * 体验仓储接口。
 */
public interface ExperienceRepository {
    TripExperience save(TripExperience experience);
    Optional<TripExperience> findById(Long id);
    List<TripExperience> findByUserId(Long userId);
    List<TripExperience> findByTripId(Long tripId);
    List<TripExperience> findByTripDayId(Long tripDayId);
    void delete(Long id);
}