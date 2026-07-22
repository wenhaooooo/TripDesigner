package com.tripdesigner.price.domain;

import java.util.List;
import java.util.Optional;

/**
 * 价格监测仓储接口。
 *
 * 定义价格监测聚合根的持久化操作。
 * 实现在 infrastructure 层（PriceMonitorRepositoryImpl），
 * 使用 MyBatis Plus 进行数据库操作。
 */
public interface PriceMonitorRepository {
    PriceMonitor save(PriceMonitor monitor);
    Optional<PriceMonitor> findById(Long id);
    List<PriceMonitor> findByUserId(Long userId);
    List<PriceMonitor> findByStatus(MonitorStatus status);
    List<PriceMonitor> findByTripId(Long tripId);
    void deleteById(Long id);
}
