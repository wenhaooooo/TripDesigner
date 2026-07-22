package com.tripdesigner.behavior.application;

import com.tripdesigner.behavior.api.dto.TrackBehaviorRequest;
import com.tripdesigner.behavior.api.vo.PreferenceProfileVo;
import com.tripdesigner.behavior.api.vo.UserBehaviorVo;
import com.tripdesigner.behavior.domain.BehaviorType;
import com.tripdesigner.behavior.domain.TargetType;
import com.tripdesigner.behavior.domain.UserBehavior;
import com.tripdesigner.behavior.domain.UserBehaviorRepository;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.memory.api.dto.PreferenceRequest;
import com.tripdesigner.memory.application.MemoryAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为应用服务。
 *
 * 职责：
 * 1. track() - 异步记录用户行为（高吞吐、不阻塞主流程）
 * 2. analyzePreferences() - 基于近期行为分析偏好画像
 * 3. syncToPreferences() - 将分析结果同步到 UserPreference（source=AI_DISCOVERED）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorAppService {

    private final UserBehaviorRepository repository;
    private final MemoryAppService memoryAppService;

    /** 异步追踪用户行为 */
    @Async
    @Transactional
    public void track(Long userId, TrackBehaviorRequest req) {
        try {
            BehaviorType behaviorType = parseEnum(BehaviorType.class, req.getBehaviorType(),
                    ResultCode.COMMON_BAD_REQUEST, "invalid behaviorType: " + req.getBehaviorType());
            TargetType targetType = parseEnum(TargetType.class, req.getTargetType(),
                    ResultCode.COMMON_BAD_REQUEST, "invalid targetType: " + req.getTargetType());

            UserBehavior behavior = UserBehavior.create(userId, behaviorType, targetType,
                    req.getTargetId(), req.getContext());
            repository.save(behavior);
        } catch (Exception e) {
            // 行为追踪失败不应影响主流程
            log.warn("[Behavior] Failed to track behavior for user {}: {}", userId, e.getMessage());
        }
    }

    /** 同步追踪（同步返回 ID，仅用于需要立即查看结果的场景） */
    @Transactional
    public UserBehaviorVo trackSync(Long userId, TrackBehaviorRequest req) {
        BehaviorType behaviorType = parseEnum(BehaviorType.class, req.getBehaviorType(),
                ResultCode.COMMON_BAD_REQUEST, "invalid behaviorType: " + req.getBehaviorType());
        TargetType targetType = parseEnum(TargetType.class, req.getTargetType(),
                ResultCode.COMMON_BAD_REQUEST, "invalid targetType: " + req.getTargetType());
        UserBehavior behavior = UserBehavior.create(userId, behaviorType, targetType,
                req.getTargetId(), req.getContext());
        UserBehavior saved = repository.save(behavior);
        return UserBehaviorVo.from(saved);
    }

    /** 列出当前用户的近期行为 */
    @Transactional(readOnly = true)
    public List<UserBehaviorVo> listMyBehaviors(int limit) {
        UserContext ctx = requireAuth();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findByUserId(ctx.userId(), safeLimit).stream()
                .map(UserBehaviorVo::from)
                .toList();
    }

    /** 分析用户偏好画像（基于近 30 天行为） */
    @Transactional(readOnly = true)
    public PreferenceProfileVo analyzePreferences(Long userId) {
        Instant since = Instant.now().minus(Duration.ofDays(30));
        List<UserBehavior> behaviors = repository.findByUserIdSince(userId, since);

        if (behaviors.isEmpty()) {
            return PreferenceProfileVo.builder()
                    .userId(userId)
                    .totalBehaviors(0L)
                    .topDestinations(List.of())
                    .topCategories(List.of())
                    .topKeywords(List.of())
                    .preferenceSummary(Map.of())
                    .recommendationHint("暂无足够行为数据生成偏好画像")
                    .build();
        }

        // 提取目的地偏好（从 context.destination 字段）
        Map<String, Integer> destinationScores = aggregateField(behaviors, "destination");
        List<Map<String, Object>> topDestinations = rankTop(destinationScores, 5, "destination");

        // 提取活动类别偏好（从 context.category 字段）
        Map<String, Integer> categoryScores = aggregateField(behaviors, "category");
        List<Map<String, Object>> topCategories = rankTop(categoryScores, 5, "category");

        // 提取搜索关键词（SEARCH 行为 + context.keyword）
        Map<String, Integer> keywordScores = behaviors.stream()
                .filter(b -> b.getBehaviorType() == BehaviorType.SEARCH)
                .map(b -> b.getContext() != null ? b.getContext().get("keyword") : null)
                .filter(Objects::nonNull)
                .filter(s -> !s.toString().isBlank())
                .collect(Collectors.groupingBy(
                        Object::toString,
                        Collectors.summingInt(s -> 1)));
        List<String> topKeywords = rankTop(keywordScores, 10, "keyword").stream()
                .map(m -> (String) m.get("keyword"))
                .toList();

        // 综合偏好摘要
        Map<String, Object> summary = new HashMap<>();
        summary.put("behaviorCount", behaviors.size());
        summary.put("destinationCount", destinationScores.size());
        summary.put("categoryCount", categoryScores.size());
        summary.put("analysisWindow", "30d");
        if (!topDestinations.isEmpty()) {
            summary.put("preferredDestination", topDestinations.get(0).get("destination"));
        }
        if (!topCategories.isEmpty()) {
            summary.put("preferredCategory", topCategories.get(0).get("category"));
        }

        // 生成 AI 推荐 Prompt 提示
        String hint = buildRecommendationHint(topDestinations, topCategories, topKeywords);

        return PreferenceProfileVo.builder()
                .userId(userId)
                .totalBehaviors(behaviors.size())
                .topDestinations(topDestinations)
                .topCategories(topCategories)
                .topKeywords(topKeywords)
                .preferenceSummary(summary)
                .recommendationHint(hint)
                .build();
    }

    /**
     * 将基于行为分析得到的偏好同步到 UserPreference（source=AI_DISCOVERED）。
     * 由定时任务或手动触发。
     */
    @Transactional
    public int syncToPreferences(Long userId) {
        PreferenceProfileVo profile = analyzePreferences(userId);
        int count = 0;

        // 同步目的地偏好
        if (!profile.getTopDestinations().isEmpty()) {
            PreferenceRequest req = new PreferenceRequest();
            req.setCategory("DESTINATION_PREFERENCE");
            Map<String, Object> data = new HashMap<>();
            data.put("topDestinations", profile.getTopDestinations());
            data.put("analysisWindow", "30d");
            data.put("behaviorCount", profile.getTotalBehaviors());
            req.setPreference(data);
            req.setSource("AI_DISCOVERED");
            try {
                memoryAppService.savePreference(userId, req);
                count++;
            } catch (Exception e) {
                log.warn("[Behavior] Failed to sync destination preference for user {}: {}", userId, e.getMessage());
            }
        }

        // 同步类别偏好
        if (!profile.getTopCategories().isEmpty()) {
            PreferenceRequest req = new PreferenceRequest();
            req.setCategory("ACTIVITY_PREFERENCE");
            Map<String, Object> data = new HashMap<>();
            data.put("topCategories", profile.getTopCategories());
            req.setPreference(data);
            req.setSource("AI_DISCOVERED");
            try {
                memoryAppService.savePreference(userId, req);
                count++;
            } catch (Exception e) {
                log.warn("[Behavior] Failed to sync category preference for user {}: {}", userId, e.getMessage());
            }
        }

        return count;
    }

    // ========== 私有方法 ==========

    /** 聚合行为 context 中的指定字段，按权重累加 */
    private Map<String, Integer> aggregateField(List<UserBehavior> behaviors, String fieldName) {
        Map<String, Integer> scores = new HashMap<>();
        for (UserBehavior b : behaviors) {
            if (b.getContext() == null) continue;
            Object value = b.getContext().get(fieldName);
            if (value == null || value.toString().isBlank()) continue;
            String key = value.toString();
            int weight = b.getWeight() != null ? b.getWeight() : 1;
            scores.merge(key, weight, Integer::sum);
        }
        return scores;
    }

    /** 按 value 降序排序并取 Top-N */
    private List<Map<String, Object>> rankTop(Map<String, Integer> scores, int limit, String fieldKey) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put(fieldKey, e.getKey());
                    m.put("score", e.getValue());
                    return m;
                })
                .toList();
    }

    private String buildRecommendationHint(List<Map<String, Object>> destinations,
                                            List<Map<String, Object>> categories,
                                            List<String> keywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("基于用户近 30 天行为分析，");
        if (!destinations.isEmpty()) {
            sb.append("偏好目的地：");
            for (int i = 0; i < Math.min(3, destinations.size()); i++) {
                sb.append(destinations.get(i).get("destination")).append("、");
            }
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        if (!categories.isEmpty()) {
            sb.append("偏好活动类型：");
            for (int i = 0; i < Math.min(3, categories.size()); i++) {
                sb.append(categories.get(i).get("category")).append("、");
            }
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        if (!keywords.isEmpty()) {
            sb.append("近期搜索关键词：").append(String.join("、", keywords.subList(0, Math.min(3, keywords.size())))).append("。");
        }
        return sb.toString();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value,
                                              ResultCode code, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(code, message);
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(code, message);
        }
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
