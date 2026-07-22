package com.tripdesigner.trip.api;
/**
 * 行程分享 REST API 控制器。
 * 提供分享链接的创建、列出、撤销操作。
 * 所有操作需认证，且只能操作当前用户拥有的行程。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.trip.api.vo.TripShareVo;
import com.tripdesigner.trip.application.TripShareAppService;
import com.tripdesigner.trip.domain.ShareType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips/{tripId}/shares")
@RequiredArgsConstructor
public class TripShareController {
    private final TripShareAppService tripShareAppService;

    /** 创建分享链接 */
    @PostMapping
    public Result<TripShareVo> createShare(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "VIEW") ShareType type,
            @RequestParam(required = false) Integer maxViews,
            @RequestParam(required = false) Integer expireDays) {
        return Result.success(tripShareAppService.createShare(tripId, type, maxViews, expireDays));
    }

    /** 列出行程的所有分享链接 */
    @GetMapping
    public Result<List<TripShareVo>> listShares(@PathVariable Long tripId) {
        return Result.success(tripShareAppService.listSharesByTrip(tripId));
    }

    /** 撤销分享链接 */
    @DeleteMapping("/{shareId}")
    public Result<Void> revokeShare(@PathVariable Long tripId, @PathVariable Long shareId) {
        tripShareAppService.revokeShare(shareId);
        return Result.success();
    }
}
