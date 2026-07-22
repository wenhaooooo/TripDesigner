package com.tripdesigner.community.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * 社区帖子领域实体（聚合根）。
 * 包含标题、内容、目的地、标签、图片 URL、互动统计等。
 */
@Getter
@Builder
public class CommunityPost {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String destination;
    private List<String> tags;
    private List<String> mediaUrls;

    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;

    private PostStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static CommunityPost create(Long userId, String title, String content,
                                         String destination, List<String> tags, List<String> mediaUrls) {
        return CommunityPost.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .destination(destination)
                .tags(tags != null ? tags : List.of())
                .mediaUrls(mediaUrls != null ? mediaUrls : List.of())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .status(PostStatus.PUBLISHED)
                .version(0)
                .build();
    }

    public CommunityPost withUpdatedFields(String title, String content, String destination,
                                             List<String> tags, List<String> mediaUrls) {
        return CommunityPost.builder()
                .id(id).userId(userId)
                .title(title != null ? title : this.title)
                .content(content != null ? content : this.content)
                .destination(destination != null ? destination : this.destination)
                .tags(tags != null ? tags : this.tags)
                .mediaUrls(mediaUrls != null ? mediaUrls : this.mediaUrls)
                .viewCount(viewCount).likeCount(likeCount).commentCount(commentCount).favoriteCount(favoriteCount)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public CommunityPost incrementViewCount() {
        return CommunityPost.builder()
                .id(id).userId(userId).title(title).content(content).destination(destination)
                .tags(tags).mediaUrls(mediaUrls)
                .viewCount((viewCount != null ? viewCount : 0) + 1)
                .likeCount(likeCount).commentCount(commentCount).favoriteCount(favoriteCount)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public CommunityPost withLikeCountDelta(int delta) {
        return CommunityPost.builder()
                .id(id).userId(userId).title(title).content(content).destination(destination)
                .tags(tags).mediaUrls(mediaUrls)
                .viewCount(viewCount)
                .likeCount(Math.max(0, (likeCount != null ? likeCount : 0) + delta))
                .commentCount(commentCount).favoriteCount(favoriteCount)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public CommunityPost withCommentCountDelta(int delta) {
        return CommunityPost.builder()
                .id(id).userId(userId).title(title).content(content).destination(destination)
                .tags(tags).mediaUrls(mediaUrls)
                .viewCount(viewCount).likeCount(likeCount)
                .commentCount(Math.max(0, (commentCount != null ? commentCount : 0) + delta))
                .favoriteCount(favoriteCount)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public CommunityPost withFavoriteCountDelta(int delta) {
        return CommunityPost.builder()
                .id(id).userId(userId).title(title).content(content).destination(destination)
                .tags(tags).mediaUrls(mediaUrls)
                .viewCount(viewCount).likeCount(likeCount).commentCount(commentCount)
                .favoriteCount(Math.max(0, (favoriteCount != null ? favoriteCount : 0) + delta))
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public CommunityPost withStatus(PostStatus newStatus) {
        return CommunityPost.builder()
                .id(id).userId(userId).title(title).content(content).destination(destination)
                .tags(tags).mediaUrls(mediaUrls)
                .viewCount(viewCount).likeCount(likeCount).commentCount(commentCount).favoriteCount(favoriteCount)
                .status(newStatus).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
