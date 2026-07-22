package com.tripdesigner.user.domain;

import java.util.Optional;

/**
 * 用户仓储接口。
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}