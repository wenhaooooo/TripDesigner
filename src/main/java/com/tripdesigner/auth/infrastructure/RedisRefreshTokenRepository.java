package com.tripdesigner.auth.infrastructure;
/**
 * 刷新令牌 Redis 仓储实现。
 * 使用 Redis 存储 Refresh Token 的哈希值，
 * 支持 TTL 自动过期和主动吊销。
 * Key 格式: auth:refresh:{userId}:{tokenId}
 */

import com.tripdesigner.auth.domain.RefreshToken;
import com.tripdesigner.auth.domain.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {
    private static final String KEY_PREFIX = "auth:refresh:";
    private final StringRedisTemplate redis;

    public RedisRefreshTokenRepository(StringRedisTemplate redis) { this.redis = redis; }

    private String key(Long userId, String tokenId) {
        return KEY_PREFIX + userId + ":" + tokenId;
    }

    @Override
    public void save(RefreshToken token, long ttlSeconds) {
        redis.opsForValue().set(key(token.getUserId(), token.getTokenId()),
                token.getTokenHash(), Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<String> findHash(Long userId, String tokenId) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId, tokenId)));
    }

    @Override
    public void revoke(Long userId, String tokenId) {
        redis.delete(key(userId, tokenId));
    }

    @Override
    public boolean exists(Long userId, String tokenId) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId, tokenId)));
    }
}
