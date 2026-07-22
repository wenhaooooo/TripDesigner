package com.tripdesigner.behavior.api;

import com.tripdesigner.behavior.api.dto.TrackBehaviorRequest;
import com.tripdesigner.behavior.api.vo.PreferenceProfileVo;
import com.tripdesigner.behavior.api.vo.UserBehaviorVo;
import com.tripdesigner.behavior.application.BehaviorAppService;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户行为追踪 REST API。
 *
 * - POST /behaviors/track           异步追踪用户行为
 * - POST /behaviors/track-sync       同步追踪（返回行为 ID）
 * - GET  /behaviors                  列出当前用户的近期行为
 * - GET  /behaviors/profile          获取当前用户的偏好画像
 * - POST /behaviors/sync-preferences  将行为分析结果同步到偏好记忆
 */
@Tag(name = "Behavior", description = "用户行为追踪与动态偏好学习")
@RestController
@RequestMapping("/behaviors")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorAppService behaviorAppService;

    @Operation(summary = "异步追踪用户行为（不阻塞主流程）")
    @PostMapping("/track")
    public Result<Void> track(@Valid @RequestBody TrackBehaviorRequest req) {
        UserContext ctx = requireAuth();
        behaviorAppService.track(ctx.userId(), req);
        return Result.success();
    }

    @Operation(summary = "同步追踪用户行为（返回行为 ID）")
    @PostMapping("/track-sync")
    public Result<UserBehaviorVo> trackSync(@Valid @RequestBody TrackBehaviorRequest req) {
        UserContext ctx = requireAuth();
        return Result.success(behaviorAppService.trackSync(ctx.userId(), req));
    }

    @Operation(summary = "列出当前用户的近期行为")
    @GetMapping
    public Result<List<UserBehaviorVo>> list(@RequestParam(defaultValue = "50") int limit) {
        UserContext ctx = requireAuth();
        return Result.success(behaviorAppService.listMyBehaviors(limit));
    }

    @Operation(summary = "分析当前用户的偏好画像（基于近 30 天行为）")
    @GetMapping("/profile")
    public Result<PreferenceProfileVo> profile() {
        UserContext ctx = requireAuth();
        return Result.success(behaviorAppService.analyzePreferences(ctx.userId()));
    }

    @Operation(summary = "将行为分析结果同步到偏好记忆（AI_DISCOVERED）")
    @PostMapping("/sync-preferences")
    public Result<Map<String, Object>> syncPreferences() {
        UserContext ctx = requireAuth();
        int count = behaviorAppService.syncToPreferences(ctx.userId());
        return Result.success(Map.of("syncedCount", count));
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
