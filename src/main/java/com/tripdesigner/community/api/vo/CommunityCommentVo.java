package com.tripdesigner.community.api.vo;

import com.tripdesigner.community.domain.CommunityComment;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class CommunityCommentVo {
    private final Long id;
    private final Long postId;
    private final Long userId;
    private final String authorEmail;
    private final Long parentId;
    private final String content;
    private final Integer likeCount;
    private final boolean likedByMe;
    private final String status;
    private final Instant createdAt;
    private final List<CommunityCommentVo> replies;

    public static CommunityCommentVo from(CommunityComment c, String authorEmail, boolean likedByMe,
                                            List<CommunityCommentVo> replies) {
        return CommunityCommentVo.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .userId(c.getUserId())
                .authorEmail(authorEmail)
                .parentId(c.getParentId())
                .content(c.getContent())
                .likeCount(c.getLikeCount())
                .likedByMe(likedByMe)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .createdAt(c.getCreatedAt())
                .replies(replies != null ? replies : List.of())
                .build();
    }
}
