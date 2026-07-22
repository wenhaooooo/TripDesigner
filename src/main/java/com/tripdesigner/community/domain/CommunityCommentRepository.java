package com.tripdesigner.community.domain;

import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepository {
    CommunityComment save(CommunityComment comment);
    Optional<CommunityComment> findById(Long id);
    List<CommunityComment> findByPostId(Long postId);
    void deleteById(Long id);
}
