package com.tripdesigner.trip.domain;

import java.util.List;
import java.util.Optional;

/**
 * 行程仓储接口。
 *
 * 定义行程聚合根的持久化操作。
 * 实现在 infrastructure 层（TripRepositoryImpl），
 * 使用 MyBatis Plus 进行数据库操作。
 */
public interface TripRepository {
    Trip save(Trip trip);
    Optional<Trip> findById(Long id);
    List<Trip> findByUserId(Long userId);

    /** 分页查询用户的行程列表 */
    List<Trip> findByUserId(Long userId, int page, int size);

    /** 查询用户的行程总数 */
    long countByUserId(Long userId);

    /** 按关键词搜索用户的行程（标题或目的地） */
    List<Trip> searchByUserId(Long userId, String keyword, int page, int size);

    /** 按关键词搜索的行程总数 */
    long countByUserIdAndKeyword(Long userId, String keyword);

    void deleteById(Long id);
}
