package com.tripdesigner.community.domain;

import java.util.List;
import java.util.Optional;

/**
 * 社区帖子仓储接口。
 */
public interface CommunityPostRepository {
    CommunityPost save(CommunityPost post);
    Optional<CommunityPost> findById(Long id);
    List<CommunityPost> findAll(int page, int size);
    List<CommunityPost> findByUserId(Long userId);
    List<CommunityPost> findByDestination(String destination, int page, int size);
    List<CommunityPost> findHot(int limit);
    long count();
    void deleteById(Long id);
}
