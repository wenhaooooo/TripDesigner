package com.tripdesigner.checkin.domain;

import java.util.List;
import java.util.Optional;

public interface TripCheckinRepository {
    TripCheckin save(TripCheckin checkin);
    Optional<TripCheckin> findById(Long id);
    List<TripCheckin> findByUserId(Long userId);
    List<TripCheckin> findByTripId(Long tripId);
    List<TripCheckin> findByActivityId(Long activityId);
    Optional<TripCheckin> findByUserAndActivity(Long userId, Long activityId);
    void deleteById(Long id);
}
