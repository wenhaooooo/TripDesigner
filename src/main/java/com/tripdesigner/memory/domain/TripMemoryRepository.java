package com.tripdesigner.memory.domain;

import java.util.List;
import java.util.Optional;

/**
 * 旅行记忆仓储接口。
 */
public interface TripMemoryRepository {
    TripMemory save(TripMemory memory);
    Optional<TripMemory> findById(Long id);
    List<TripMemory> findByUserId(Long userId);
    List<TripMemory> findByUserIdAndType(Long userId, String memoryType);
    void delete(Long id);
}