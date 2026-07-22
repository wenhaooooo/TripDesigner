package com.tripdesigner.community.domain;

import java.util.Optional;

public interface CommunityLikeRepository {
    CommunityLike save(CommunityLike like);
    Optional<CommunityLike> findByUserAndTarget(Long userId, Long targetId, LikeTargetType targetType);
    void delete(Long userId, Long targetId, LikeTargetType targetType);
    boolean exists(Long userId, Long targetId, LikeTargetType targetType);
}
