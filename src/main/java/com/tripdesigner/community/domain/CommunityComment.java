package com.tripdesigner.community.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 社区评论领域实体。
 * 支持多级评论（parent_id 指向父评论）。
 */
@Getter
@Builder
public class CommunityComment {

    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private PostStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static CommunityComment create(Long postId, Long userId, Long parentId, String content) {
        return CommunityComment.builder()
                .postId(postId)
                .userId(userId)
                .parentId(parentId)
                .content(content)
                .likeCount(0)
                .status(PostStatus.PUBLISHED)
                .version(0)
                .build();
    }

    public CommunityComment withLikeCountDelta(int delta) {
        return CommunityComment.builder()
                .id(id).postId(postId).userId(userId).parentId(parentId)
                .content(content)
                .likeCount(Math.max(0, (likeCount != null ? likeCount : 0) + delta))
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
