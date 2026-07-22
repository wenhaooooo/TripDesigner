package com.tripdesigner.behavior.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 用户行为领域实体。
 * 记录用户在系统中的所有可观测行为，作为偏好学习的输入数据。
 */
@Getter
@Builder
public class UserBehavior {

    private Long id;
    private Long userId;
    private BehaviorType behaviorType;
    private TargetType targetType;
    private Long targetId;
    /** 附加上下文 JSONB（如搜索关键词、查看时长等） */
    private Map<String, Object> context;
    /** 行为权重：不同行为权重不同，用于偏好计算 */
    private Integer weight;
    private Instant createdAt;

    public static UserBehavior create(Long userId, BehaviorType behaviorType, TargetType targetType,
                                       Long targetId, Map<String, Object> context) {
        return UserBehavior.builder()
                .userId(userId)
                .behaviorType(behaviorType)
                .targetType(targetType)
                .targetId(targetId)
                .context(context != null ? context : Map.of())
                .weight(defaultWeight(behaviorType))
                .build();
    }

    /** 不同行为类型的默认权重：高价值行为权重大 */
    public static int defaultWeight(BehaviorType type) {
        return switch (type) {
            case VIEW -> 1;
            case CLICK -> 2;
            case SEARCH -> 3;
            case SHARE -> 4;
            case SAVE, FAVORITE -> 5;
            case BOOK -> 10;
            case CANCEL -> -3;
            case SKIP -> -1;
        };
    }
}
