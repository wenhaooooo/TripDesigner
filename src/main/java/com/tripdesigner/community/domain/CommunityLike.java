package com.tripdesigner.community.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 点赞领域实体（用于帖子/评论的点赞记录）。
 */
@Getter
@Builder
public class CommunityLike {

    private Long id;
    private Long userId;
    private Long targetId;
    private LikeTargetType targetType;
    private Instant createdAt;

    public static CommunityLike create(Long userId, Long targetId, LikeTargetType targetType) {
        return CommunityLike.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(targetType)
                .build();
    }
}
