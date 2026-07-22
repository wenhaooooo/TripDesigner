package com.tripdesigner.trip.domain;

import java.util.List;
import java.util.Optional;

/**
 * 行程分享仓储接口。
 *
 * 定义 TripShare（分享链接）的持久化操作。
 * 实现在 infrastructure 层（TripShareRepositoryImpl），
 * 使用 MyBatis Plus 进行数据库操作。
 */
public interface TripShareRepository {
    TripShare save(TripShare tripShare);
    Optional<TripShare> findByToken(String token);
    Optional<TripShare> findById(Long id);
    List<TripShare> findByTripId(Long tripId);
    void deleteById(Long id);
}
