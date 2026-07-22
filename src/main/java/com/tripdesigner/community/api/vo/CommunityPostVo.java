package com.tripdesigner.community.api.vo;

import com.tripdesigner.community.domain.CommunityPost;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class CommunityPostVo {
    private final Long id;
    private final Long userId;
    private final String authorEmail;
    private final String title;
    private final String content;
    private final String destination;
    private final List<String> tags;
    private final List<String> mediaUrls;
    private final Integer viewCount;
    private final Integer likeCount;
    private final Integer commentCount;
    private final Integer favoriteCount;
    private final boolean likedByMe;
    private final boolean favoritedByMe;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static CommunityPostVo from(CommunityPost p, String authorEmail, boolean likedByMe, boolean favoritedByMe) {
        return CommunityPostVo.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .authorEmail(authorEmail)
                .title(p.getTitle())
                .content(p.getContent())
                .destination(p.getDestination())
                .tags(p.getTags())
                .mediaUrls(p.getMediaUrls())
                .viewCount(p.getViewCount())
                .likeCount(p.getLikeCount())
                .commentCount(p.getCommentCount())
                .favoriteCount(p.getFavoriteCount())
                .likedByMe(likedByMe)
                .favoritedByMe(favoritedByMe)
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
