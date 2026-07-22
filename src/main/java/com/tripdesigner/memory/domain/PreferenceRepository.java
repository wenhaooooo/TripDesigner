package com.tripdesigner.memory.domain;

import java.util.List;
import java.util.Optional;

/**
 * 用户偏好仓储接口。
 */
public interface PreferenceRepository {
    UserPreference save(UserPreference preference);
    Optional<UserPreference> findById(Long id);
    List<UserPreference> findByUserId(Long userId);
    List<UserPreference> findByUserIdAndCategory(Long userId, String category);
    void delete(Long id);
}