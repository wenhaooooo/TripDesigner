package com.tripdesigner.community.domain;

import java.util.List;
import java.util.Optional;

public interface CommunityFavoriteRepository {
    CommunityFavorite save(CommunityFavorite favorite);
    Optional<CommunityFavorite> findByUserAndPost(Long userId, Long postId);
    List<CommunityFavorite> findByUserId(Long userId);
    void delete(Long userId, Long postId);
    boolean exists(Long userId, Long postId);
}
