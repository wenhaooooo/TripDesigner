package com.tripdesigner.checkin.api;

import com.tripdesigner.checkin.api.dto.CreateCheckinRequest;
import com.tripdesigner.checkin.api.vo.CheckinStatsVo;
import com.tripdesigner.checkin.api.vo.TripCheckinVo;
import com.tripdesigner.checkin.application.CheckinAppService;
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

/**
 * 行程签到 REST API。
 *
 * - POST   /checkins             创建签到
 * - GET    /checkins              我的签到列表
 * - GET    /checkins/trip/{tripId} 按行程列出签到
 * - GET    /checkins/{id}         签到详情
 * - PUT    /checkins/{id}/status 更新签到状态
 * - DELETE /checkins/{id}         删除签到
 * - GET    /checkins/stats        打卡统计
 */
@Tag(name = "Checkin", description = "行程签到与打卡")
@RestController
@RequestMapping("/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinAppService checkinAppService;

    @Operation(summary = "创建签到")
    @PostMapping
    public Result<TripCheckinVo> create(@Valid @RequestBody CreateCheckinRequest req) {
        requireAuth();
        return Result.success(checkinAppService.checkin(req));
    }

    @Operation(summary = "我的签到列表")
    @GetMapping
    public Result<List<TripCheckinVo>> listMine() {
        requireAuth();
        return Result.success(checkinAppService.listMyCheckins());
    }

    @Operation(summary = "按行程列出签到")
    @GetMapping("/trip/{tripId}")
    public Result<List<TripCheckinVo>> listByTrip(@PathVariable Long tripId) {
        return Result.success(checkinAppService.listByTrip(tripId));
    }

    @Operation(summary = "签到详情")
    @GetMapping("/{id}")
    public Result<TripCheckinVo> getById(@PathVariable Long id) {
        requireAuth();
        return Result.success(checkinAppService.getById(id));
    }

    @Operation(summary = "更新签到状态（COMPLETED/SKIPPED）")
    @PutMapping("/{id}/status")
    public Result<TripCheckinVo> updateStatus(@PathVariable Long id,
                                                @RequestParam String status) {
        requireAuth();
        return Result.success(checkinAppService.updateStatus(id, status));
    }

    @Operation(summary = "删除签到")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAuth();
        checkinAppService.delete(id);
        return Result.success();
    }

    @Operation(summary = "打卡统计")
    @GetMapping("/stats")
    public Result<CheckinStatsVo> stats() {
        requireAuth();
        return Result.success(checkinAppService.getStats());
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
