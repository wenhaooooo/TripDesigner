package com.tripdesigner.community.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.community.api.dto.CreateCommentRequest;
import com.tripdesigner.community.api.dto.CreatePostRequest;
import com.tripdesigner.community.api.dto.UpdatePostRequest;
import com.tripdesigner.community.api.vo.CommunityCommentVo;
import com.tripdesigner.community.api.vo.CommunityPostVo;
import com.tripdesigner.community.domain.*;
import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 旅行社区应用服务。
 *
 * 编排帖子、评论、点赞、收藏的业务用例。
 * 所有面向用户的操作均要求认证；列表/详情读取公开访问（在 SecurityConfig 配置）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityAppService {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityLikeRepository likeRepository;
    private final CommunityFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    // ========== 帖子 ==========

    @Transactional
    public CommunityPostVo createPost(CreatePostRequest req) {
        UserContext ctx = requireAuth();
        CommunityPost post = CommunityPost.create(
                ctx.userId(), req.getTitle(), req.getContent(),
                req.getDestination(), req.getTags(), req.getMediaUrls());
        CommunityPost saved = postRepository.save(post);
        return toPostVo(saved, ctx.userId());
    }

    @Transactional(readOnly = true)
    public PageResult<CommunityPostVo> listPosts(int page, int size) {
        Long currentUserId = currentUserIdOrNull();
        List<CommunityPost> posts = postRepository.findAll(page, size);
        List<CommunityPostVo> vos = posts.stream()
                .map(p -> toPostVo(p, currentUserId))
                .toList();
        return new PageResult<>(page, size, postRepository.count(), vos);
    }

    @Transactional(readOnly = true)
    public List<CommunityPostVo> listHotPosts(int limit) {
        Long currentUserId = currentUserIdOrNull();
        return postRepository.findHot(limit).stream()
                .map(p -> toPostVo(p, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostVo> listByDestination(String destination, int page, int size) {
        Long currentUserId = currentUserIdOrNull();
        return postRepository.findByDestination(destination, page, size).stream()
                .map(p -> toPostVo(p, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostVo> listMyPosts() {
        UserContext ctx = requireAuth();
        return postRepository.findByUserId(ctx.userId()).stream()
                .map(p -> toPostVo(p, ctx.userId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostVo> listMyFavorites() {
        UserContext ctx = requireAuth();
        return favoriteRepository.findByUserId(ctx.userId()).stream()
                .map(f -> postRepository.findById(f.getPostId()).orElse(null))
                .filter(Objects::nonNull)
                .map(p -> toPostVo(p, ctx.userId()))
                .toList();
    }

    @Transactional
    public CommunityPostVo getPost(Long id) {
        Long currentUserId = currentUserIdOrNull();
        CommunityPost post = loadPost(id);
        // 增加浏览量
        CommunityPost viewed = postRepository.save(post.incrementViewCount());
        return toPostVo(viewed, currentUserId);
    }

    @Transactional
    public CommunityPostVo updatePost(Long id, UpdatePostRequest req) {
        UserContext ctx = requireAuth();
        CommunityPost post = loadOwnedPost(id, ctx.userId());
        CommunityPost updated = post.withUpdatedFields(
                req.getTitle(), req.getContent(), req.getDestination(), req.getTags(), req.getMediaUrls());
        CommunityPost saved = postRepository.save(updated);
        return toPostVo(saved, ctx.userId());
    }

    @Transactional
    public void deletePost(Long id) {
        UserContext ctx = requireAuth();
        CommunityPost post = loadOwnedPost(id, ctx.userId());
        postRepository.deleteById(post.getId());
    }

    // ========== 评论 ==========

    @Transactional
    public CommunityCommentVo createComment(Long postId, CreateCommentRequest req) {
        UserContext ctx = requireAuth();
        CommunityPost post = loadPost(postId);
        CommunityComment comment = CommunityComment.create(postId, ctx.userId(), req.getParentId(), req.getContent());
        CommunityComment saved = commentRepository.save(comment);

        // 增加帖子评论数
        postRepository.save(post.withCommentCountDelta(1));

        String authorEmail = userEmail(ctx.userId()).orElse("");
        boolean likedByMe = false;
        return CommunityCommentVo.from(saved, authorEmail, likedByMe, List.of());
    }

    @Transactional(readOnly = true)
    public List<CommunityCommentVo> listComments(Long postId) {
        Long currentUserId = currentUserIdOrNull();
        List<CommunityComment> all = commentRepository.findByPostId(postId);
        // 构建两层结构：顶层评论 + 回复
        Map<Long, List<CommunityComment>> byParent = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommunityComment::getParentId));

        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> {
                    String authorEmail = userEmail(c.getUserId()).orElse("");
                    boolean likedByMe = currentUserId != null
                            && likeRepository.exists(currentUserId, c.getId(), LikeTargetType.COMMENT);
                    List<CommunityCommentVo> replies = byParent.getOrDefault(c.getId(), List.of()).stream()
                            .map(r -> {
                                String replyEmail = userEmail(r.getUserId()).orElse("");
                                boolean replyLiked = currentUserId != null
                                        && likeRepository.exists(currentUserId, r.getId(), LikeTargetType.COMMENT);
                                return CommunityCommentVo.from(r, replyEmail, replyLiked, List.of());
                            })
                            .toList();
                    return CommunityCommentVo.from(c, authorEmail, likedByMe, replies);
                })
                .toList();
    }

    @Transactional
    public void deleteComment(Long commentId) {
        UserContext ctx = requireAuth();
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "comment not found"));
        if (!comment.getUserId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "comment does not belong to user");
        }
        commentRepository.deleteById(commentId);
        // 减少帖子评论数
        postRepository.findById(comment.getPostId()).ifPresent(post ->
                postRepository.save(post.withCommentCountDelta(-1)));
    }

    // ========== 点赞 ==========

    @Transactional
    public boolean togglePostLike(Long postId) {
        UserContext ctx = requireAuth();
        CommunityPost post = loadPost(postId);
        boolean exists = likeRepository.exists(ctx.userId(), postId, LikeTargetType.POST);
        if (exists) {
            likeRepository.delete(ctx.userId(), postId, LikeTargetType.POST);
            postRepository.save(post.withLikeCountDelta(-1));
            return false;
        } else {
            likeRepository.save(CommunityLike.create(ctx.userId(), postId, LikeTargetType.POST));
            postRepository.save(post.withLikeCountDelta(1));
            return true;
        }
    }

    @Transactional
    public boolean toggleCommentLike(Long commentId) {
        UserContext ctx = requireAuth();
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "comment not found"));
        boolean exists = likeRepository.exists(ctx.userId(), commentId, LikeTargetType.COMMENT);
        if (exists) {
            likeRepository.delete(ctx.userId(), commentId, LikeTargetType.COMMENT);
            commentRepository.save(comment.withLikeCountDelta(-1));
            return false;
        } else {
            likeRepository.save(CommunityLike.create(ctx.userId(), commentId, LikeTargetType.COMMENT));
            commentRepository.save(comment.withLikeCountDelta(1));
            return true;
        }
    }

    // ========== 收藏 ==========

    @Transactional
    public boolean toggleFavorite(Long postId) {
        UserContext ctx = requireAuth();
        CommunityPost post = loadPost(postId);
        boolean exists = favoriteRepository.exists(ctx.userId(), postId);
        if (exists) {
            favoriteRepository.delete(ctx.userId(), postId);
            postRepository.save(post.withFavoriteCountDelta(-1));
            return false;
        } else {
            favoriteRepository.save(CommunityFavorite.create(ctx.userId(), postId));
            postRepository.save(post.withFavoriteCountDelta(1));
            return true;
        }
    }

    // ========== 私有方法 ==========

    private CommunityPostVo toPostVo(CommunityPost post, Long currentUserId) {
        String authorEmail = userEmail(post.getUserId()).orElse("");
        boolean likedByMe = currentUserId != null
                && likeRepository.exists(currentUserId, post.getId(), LikeTargetType.POST);
        boolean favoritedByMe = currentUserId != null
                && favoriteRepository.exists(currentUserId, post.getId());
        return CommunityPostVo.from(post, authorEmail, likedByMe, favoritedByMe);
    }

    private CommunityPost loadPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "post not found"));
    }

    private CommunityPost loadOwnedPost(Long id, Long currentUserId) {
        CommunityPost post = loadPost(id);
        if (!post.getUserId().equals(currentUserId)) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "post does not belong to user");
        }
        return post;
    }

    private Optional<String> userEmail(Long userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId).map(User::getEmail);
    }

    private Long currentUserIdOrNull() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.userId() : null;
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
