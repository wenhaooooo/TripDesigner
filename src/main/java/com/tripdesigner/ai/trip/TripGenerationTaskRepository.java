package com.tripdesigner.ai.trip;

import java.util.List;
import java.util.Optional;

public interface TripGenerationTaskRepository {
    TripGenerationTask save(TripGenerationTask task);
    Optional<TripGenerationTask> findById(Long id);
    List<TripGenerationTask> findByUserId(Long userId);
    List<TripGenerationTask> findByUserIdAndStatus(Long userId, TripGenerationStatus status);
    List<TripGenerationTask> findByTripId(Long tripId);
    void deleteById(Long id);
}