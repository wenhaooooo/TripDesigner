package com.tripdesigner.behavior.api.vo;

import com.tripdesigner.behavior.domain.UserBehavior;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class UserBehaviorVo {
    private final Long id;
    private final Long userId;
    private final String behaviorType;
    private final String targetType;
    private final Long targetId;
    private final Map<String, Object> context;
    private final Integer weight;
    private final Instant createdAt;

    public static UserBehaviorVo from(UserBehavior b) {
        return UserBehaviorVo.builder()
                .id(b.getId())
                .userId(b.getUserId())
                .behaviorType(b.getBehaviorType() != null ? b.getBehaviorType().name() : null)
                .targetType(b.getTargetType() != null ? b.getTargetType().name() : null)
                .targetId(b.getTargetId())
                .context(b.getContext())
                .weight(b.getWeight())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
