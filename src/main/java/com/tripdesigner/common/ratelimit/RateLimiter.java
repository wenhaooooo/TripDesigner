package com.tripdesigner.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Bucket4j 的本地内存限流器。
 *
 * 策略：
 * - 按用户 ID（已登录）或客户端 IP（匿名）维度限流
 * - 默认每个桶每分钟 30 次请求（可配置）
 * - 超出限制返回 429 Too Many Requests
 *
 * 内存管理：
 * - 每个桶记录最后访问时间，{@link #cleanupStaleBuckets()} 定时清理超过 TTL 未访问的桶
 * - 防止匿名用户/IP 轮换导致 buckets Map 无限增长
 *
 * 注意：本实现为单机限流。生产环境多实例部署需替换为 Redis 分布式限流。
 */
@Slf4j
@Component
public class RateLimiter {

    private static final int DEFAULT_CAPACITY = 200;
    private static final int DEFAULT_REFILL_PER_MINUTE = 200;
    /** 桶的最大空闲时间，超过后会被清理（与限流窗口 1min 相比有充裕余量） */
    private static final Duration BUCKET_TTL = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    private final int capacity;
    private final int refillPerMinute;

    public RateLimiter() {
        this(DEFAULT_CAPACITY, DEFAULT_REFILL_PER_MINUTE);
    }

    public RateLimiter(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    /**
     * 尝试消费 1 个令牌。
     *
     * @param key 限流维度（用户 ID 或 IP）
     * @return true 允许通过；false 超出限制
     */
    public boolean tryConsume(String key) {
        BucketEntry entry = buckets.computeIfAbsent(key, k -> new BucketEntry(newBucket(capacity, refillPerMinute)));
        entry.touch();
        boolean allowed = entry.bucket.tryConsume(1);
        if (!allowed) {
            log.warn("[RateLimiter] Rate limit exceeded for key={}", key);
        }
        return allowed;
    }

    /**
     * 为 AI 工作流等重资源接口单独配置限流。
     */
    public boolean tryConsumeAi(String key) {
        BucketEntry entry = buckets.computeIfAbsent("ai:" + key, k -> new BucketEntry(newBucket(30, 30)));
        entry.touch();
        boolean allowed = entry.bucket.tryConsume(1);
        if (!allowed) {
            log.warn("[RateLimiter] AI rate limit exceeded for key={}", key);
        }
        return allowed;
    }

    /**
     * 定时清理超过 TTL 未访问的桶，防止 buckets Map 无限增长。
     * 每 5 分钟执行一次，清理 10 分钟未活动的桶。
     */
    @Scheduled(fixedDelay = 300_000)
    public void cleanupStaleBuckets() {
        long cutoff = System.currentTimeMillis() - BUCKET_TTL.toMillis();
        int removed = 0;
        Iterator<Map.Entry<String, BucketEntry>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BucketEntry> e = it.next();
            if (e.getValue().lastAccessed.get() < cutoff) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("[RateLimiter] Cleaned up {} stale buckets, remaining={}", removed, buckets.size());
        }
    }

    private Bucket newBucket(int capacity, int refillPerMinute) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /** 桶及其最后访问时间戳 */
    private static final class BucketEntry {
        final Bucket bucket;
        final AtomicLong lastAccessed;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessed = new AtomicLong(System.currentTimeMillis());
        }

        void touch() {
            lastAccessed.set(System.currentTimeMillis());
        }
    }
}
