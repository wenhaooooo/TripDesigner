package com.tripdesigner.community.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.community.api.dto.CreateCommentRequest;
import com.tripdesigner.community.api.dto.CreatePostRequest;
import com.tripdesigner.community.api.dto.UpdatePostRequest;
import com.tripdesigner.community.api.vo.CommunityCommentVo;
import com.tripdesigner.community.api.vo.CommunityPostVo;
import com.tripdesigner.community.application.CommunityAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 旅行社区 REST API。
 *
 * 端点：
 * - POST   /community/posts                  创建帖子
 * - GET    /community/posts                   分页列出帖子
 * - GET    /community/posts/hot               获取热门帖子
 * - GET    /community/posts/destination       按目的地过滤
 * - GET    /community/posts/mine             我的帖子
 * - GET    /community/posts/favorites         我的收藏
 * - GET    /community/posts/{id}              查看帖子详情
 * - PUT    /community/posts/{id}              更新帖子
 * - DELETE /community/posts/{id}              删除帖子
 * - POST   /community/posts/{id}/like        切换帖子点赞
 * - POST   /community/posts/{id}/favorite    切换收藏
 * - GET    /community/posts/{id}/comments     列出评论
 * - POST   /community/posts/{id}/comments     创建评论
 * - DELETE /community/comments/{id}           删除评论
 * - POST   /community/comments/{id}/like     切换评论点赞
 */
@Tag(name = "Community", description = "旅行社区：帖子、评论、点赞、收藏")
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityAppService communityAppService;

    @Operation(summary = "创建帖子")
    @PostMapping("/posts")
    public Result<CommunityPostVo> createPost(@Valid @RequestBody CreatePostRequest req) {
        requireAuth();
        return Result.success(communityAppService.createPost(req));
    }

    @Operation(summary = "分页列出帖子")
    @GetMapping("/posts")
    public Result<PageResult<CommunityPostVo>> listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(communityAppService.listPosts(page, size));
    }

    @Operation(summary = "获取热门帖子")
    @GetMapping("/posts/hot")
    public Result<List<CommunityPostVo>> listHotPosts(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(communityAppService.listHotPosts(limit));
    }

    @Operation(summary = "按目的地过滤帖子")
    @GetMapping("/posts/destination")
    public Result<List<CommunityPostVo>> listByDestination(
            @RequestParam String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(communityAppService.listByDestination(destination, page, size));
    }

    @Operation(summary = "我的帖子")
    @GetMapping("/posts/mine")
    public Result<List<CommunityPostVo>> listMyPosts() {
        requireAuth();
        return Result.success(communityAppService.listMyPosts());
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/posts/favorites")
    public Result<List<CommunityPostVo>> listMyFavorites() {
        requireAuth();
        return Result.success(communityAppService.listMyFavorites());
    }

    @Operation(summary = "查看帖子详情")
    @GetMapping("/posts/{id}")
    public Result<CommunityPostVo> getPost(@PathVariable Long id) {
        return Result.success(communityAppService.getPost(id));
    }

    @Operation(summary = "更新帖子")
    @PutMapping("/posts/{id}")
    public Result<CommunityPostVo> updatePost(@PathVariable Long id,
                                                @Valid @RequestBody UpdatePostRequest req) {
        requireAuth();
        return Result.success(communityAppService.updatePost(id, req));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        requireAuth();
        communityAppService.deletePost(id);
        return Result.success();
    }

    @Operation(summary = "切换帖子点赞（已赞则取消，未赞则点赞）")
    @PostMapping("/posts/{id}/like")
    public Result<Map<String, Object>> togglePostLike(@PathVariable Long id) {
        requireAuth();
        boolean liked = communityAppService.togglePostLike(id);
        return Result.success(Map.of("liked", liked));
    }

    @Operation(summary = "切换收藏")
    @PostMapping("/posts/{id}/favorite")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long id) {
        requireAuth();
        boolean favorited = communityAppService.toggleFavorite(id);
        return Result.success(Map.of("favorited", favorited));
    }

    @Operation(summary = "列出帖子评论")
    @GetMapping("/posts/{id}/comments")
    public Result<List<CommunityCommentVo>> listComments(@PathVariable Long id) {
        return Result.success(communityAppService.listComments(id));
    }

    @Operation(summary = "创建评论")
    @PostMapping("/posts/{id}/comments")
    public Result<CommunityCommentVo> createComment(@PathVariable Long id,
                                                       @Valid @RequestBody CreateCommentRequest req) {
        requireAuth();
        return Result.success(communityAppService.createComment(id, req));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        requireAuth();
        communityAppService.deleteComment(id);
        return Result.success();
    }

    @Operation(summary = "切换评论点赞")
    @PostMapping("/comments/{id}/like")
    public Result<Map<String, Object>> toggleCommentLike(@PathVariable Long id) {
        requireAuth();
        boolean liked = communityAppService.toggleCommentLike(id);
        return Result.success(Map.of("liked", liked));
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
