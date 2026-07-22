package com.tripdesigner.memory.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.memory.api.dto.PreferenceRequest;
import com.tripdesigner.memory.api.dto.TripMemoryRequest;
import com.tripdesigner.memory.api.vo.MemorySummaryVo;
import com.tripdesigner.memory.api.vo.PreferenceVo;
import com.tripdesigner.memory.api.vo.TripMemoryVo;
import com.tripdesigner.memory.domain.PreferenceRepository;
import com.tripdesigner.memory.domain.PreferenceSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.tripdesigner.memory.domain.TripMemory;
import com.tripdesigner.memory.domain.TripMemoryRepository;
import com.tripdesigner.memory.domain.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Application service for user preferences and trip memories.
 */
@Service
@RequiredArgsConstructor
public class MemoryAppService {

    private final PreferenceRepository preferenceRepository;
    private final TripMemoryRepository tripMemoryRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String PREF_CACHE_PREFIX = "memory:pref:";
    private static final String MEM_CACHE_PREFIX = "memory:trip:";
    private static final long CACHE_TTL_SECONDS = 300; // 5 min

    // ==================== User Preferences ====================

    @Transactional
    public PreferenceVo savePreference(PreferenceRequest request) {
        UserContext ctx = requireAuth();
        return savePreference(ctx.userId(), request);
    }

    @Transactional
    public PreferenceVo savePreference(Long userId, PreferenceRequest request) {
        PreferenceSource source = PreferenceSource.MANUAL;
        if (request.getSource() != null) {
            try {
                source = PreferenceSource.valueOf(request.getSource());
            } catch (IllegalArgumentException e) {
                source = PreferenceSource.MANUAL;
            }
        }

        UserPreference preference = UserPreference.create(
                userId, request.getCategory(), request.getPreference(), source
        );

        preference = preferenceRepository.save(preference);
        invalidatePreferenceCache(userId);
        return toPreferenceVo(preference);
    }

    public List<PreferenceVo> listPreferences() {
        UserContext ctx = requireAuth();
        return preferenceRepository.findByUserId(ctx.userId()).stream()
                .map(this::toPreferenceVo)
                .toList();
    }

    public PreferenceVo getPreference(Long preferenceId) {
        UserContext ctx = requireAuth();

        UserPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new BizException(ResultCode.PREFERENCE_NOT_FOUND, "Preference not found"));

        if (!preference.getUserId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }

        return toPreferenceVo(preference);
    }

    @Transactional
    public void deletePreference(Long preferenceId) {
        UserContext ctx = requireAuth();

        UserPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new BizException(ResultCode.PREFERENCE_NOT_FOUND, "Preference not found"));

        if (!preference.getUserId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }

        preferenceRepository.delete(preferenceId);
        invalidatePreferenceCache(ctx.userId());
    }

    // ==================== Trip Memories ====================

    @Transactional
    public TripMemoryVo saveTripMemory(TripMemoryRequest request) {
        UserContext ctx = requireAuth();
        return saveTripMemory(ctx.userId(), request);
    }

    @Transactional
    public TripMemoryVo saveTripMemory(Long userId, TripMemoryRequest request) {
        TripMemory memory = TripMemory.create(
                userId, request.getTripId(), request.getMemoryType(),
                request.getContent(), request.getTags()
        );

        memory = tripMemoryRepository.save(memory);
        invalidateMemoryCache(userId);
        return toTripMemoryVo(memory);
    }

    public List<TripMemoryVo> listTripMemories() {
        UserContext ctx = requireAuth();
        return tripMemoryRepository.findByUserId(ctx.userId()).stream()
                .map(this::toTripMemoryVo)
                .toList();
    }

    public List<TripMemoryVo> listTripMemoriesByType(String memoryType) {
        UserContext ctx = requireAuth();
        return tripMemoryRepository.findByUserIdAndType(ctx.userId(), memoryType).stream()
                .map(this::toTripMemoryVo)
                .toList();
    }

    // ==================== Memory Retrieval for AI ====================

    /**
     * Retrieve a summary of user preferences formatted for AI agent prompts.
     * Result is cached in Redis for 5 minutes to avoid repeated DB queries
     * when multiple agents request the same data.
     */
    public String getPreferenceSummary(Long userId) {
        String cacheKey = PREF_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<UserPreference> preferences = preferenceRepository.findByUserId(userId);
        String summary;
        if (preferences.isEmpty()) {
            summary = "No user preferences available.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("User Preferences:\n");
            for (UserPreference p : preferences) {
                sb.append("- ").append(p.getCategory()).append(": ");
                for (Map.Entry<String, Object> entry : p.getData().entrySet()) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                }
                sb.delete(sb.length() - 2, sb.length()).append("\n");
            }
            summary = sb.toString();
        }

        redisTemplate.opsForValue().set(cacheKey, summary, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
        return summary;
    }

    /**
     * Retrieve trip memories formatted for AI agent prompts.
     * Result is cached in Redis to avoid repeated DB queries.
     */
    public String getTripMemorySummary(Long userId) {
        String cacheKey = MEM_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<TripMemory> memories = tripMemoryRepository.findByUserId(userId);
        String summary;
        if (memories.isEmpty()) {
            summary = "No trip memories available.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Past Trip Memories:\n");
            for (TripMemory m : memories) {
                sb.append("- [").append(m.getMemoryType()).append("] ").append(m.getContent()).append("\n");
            }
            summary = sb.toString();
        }

        redisTemplate.opsForValue().set(cacheKey, summary, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
        return summary;
    }

    /**
     * Invalidate preference cache for a user (called after saving/deleting preferences).
     */
    public void invalidatePreferenceCache(Long userId) {
        redisTemplate.delete(PREF_CACHE_PREFIX + userId);
    }

    /**
     * Invalidate trip memory cache for a user.
     */
    public void invalidateMemoryCache(Long userId) {
        redisTemplate.delete(MEM_CACHE_PREFIX + userId);
    }

    /**
     * Get memory summary for the authenticated user.
     */
    public MemorySummaryVo getMemorySummary() {
        UserContext ctx = requireAuth();
        String preferenceSummary = getPreferenceSummary(ctx.userId());
        String tripMemorySummary = getTripMemorySummary(ctx.userId());

        MemorySummaryVo summary = new MemorySummaryVo();
        summary.setPreferenceSummary(preferenceSummary);
        summary.setTripMemorySummary(tripMemorySummary);
        return summary;
    }

    // ==================== Private helpers ====================

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private PreferenceVo toPreferenceVo(UserPreference preference) {
        PreferenceVo vo = new PreferenceVo();
        vo.setId(preference.getId());
        vo.setCategory(preference.getCategory());
        vo.setData(preference.getData());
        vo.setSource(preference.getSource().name());
        vo.setCreatedAt(preference.getCreatedAt());
        vo.setUpdatedAt(preference.getUpdatedAt());
        return vo;
    }

    private TripMemoryVo toTripMemoryVo(TripMemory memory) {
        TripMemoryVo vo = new TripMemoryVo();
        vo.setId(memory.getId());
        vo.setTripId(memory.getTripId());
        vo.setMemoryType(memory.getMemoryType());
        vo.setContent(memory.getContent());
        vo.setTags(memory.getTags());
        vo.setCreatedAt(memory.getCreatedAt());
        return vo;
    }
}
