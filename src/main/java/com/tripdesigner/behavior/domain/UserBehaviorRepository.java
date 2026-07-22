package com.tripdesigner.behavior.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 用户行为仓储接口。
 */
public interface UserBehaviorRepository {
    UserBehavior save(UserBehavior behavior);
    Optional<UserBehavior> findById(Long id);
    List<UserBehavior> findByUserId(Long userId, int limit);
    List<UserBehavior> findByUserIdSince(Long userId, Instant since);
    List<UserBehavior> findByUserIdAndType(Long userId, BehaviorType type, int limit);
    long countByUserId(Long userId);
}
