package com.tripdesigner.trip.domain;

import java.util.List;
import java.util.Optional;

/**
 * 行程日仓储接口。
 *
 * 定义 TripDay（行程日）的持久化操作。
 * 一个 Trip（行程）包含多个 TripDay，
 * 通过 findByTripId 按行程 ID 查询所有关联的行程日。
 */
public interface TripDayRepository {
    TripDay save(TripDay tripDay);
    Optional<TripDay> findById(Long id);
    List<TripDay> findByTripId(Long tripId);
    void deleteById(Long id);
}
