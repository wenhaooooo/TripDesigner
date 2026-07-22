package com.tripdesigner.trip.domain;

import java.util.List;
import java.util.Optional;

/**
 * 活动仓储接口。
 *
 * 定义 TripActivity（活动）的持久化操作。
 * 一个 TripDay（行程日）包含多个 TripActivity，
 * 通过 findByTripDayId 按行程日 ID 查询所有关联的活动。
 */
public interface TripActivityRepository {
    TripActivity save(TripActivity activity);
    Optional<TripActivity> findById(Long id);
    List<TripActivity> findByTripDayId(Long tripDayId);
    void deleteById(Long id);
}
