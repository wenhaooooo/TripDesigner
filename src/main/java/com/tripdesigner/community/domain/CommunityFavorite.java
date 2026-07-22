package com.tripdesigner.community.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 收藏领域实体。
 */
@Getter
@Builder
public class CommunityFavorite {

    private Long id;
    private Long userId;
    private Long postId;
    private Instant createdAt;

    public static CommunityFavorite create(Long userId, Long postId) {
        return CommunityFavorite.builder()
                .userId(userId)
                .postId(postId)
                .build();
    }
}
