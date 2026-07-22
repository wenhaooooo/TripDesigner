package com.tripdesigner.auth.domain;

import java.util.Optional;

/**
 * 刷新令牌仓储接口。
 * 实现在 infrastructure 层（RedisRefreshTokenRepository），
 * 使用 Redis 存储令牌哈希，支持 TTL 自动过期。
 */
public interface RefreshTokenRepository {
    void save(RefreshToken token, long ttlSeconds);
    Optional<String> findHash(Long userId, String tokenId);
    void revoke(Long userId, String tokenId);
    boolean exists(Long userId, String tokenId);
}