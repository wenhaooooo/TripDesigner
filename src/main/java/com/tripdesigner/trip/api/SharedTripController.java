package com.tripdesigner.trip.api;
/**
 * 公开分享行程 REST API 控制器。
 * 通过分享 token 查看行程详情，不需要认证。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.application.TripShareAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shared")
@RequiredArgsConstructor
public class SharedTripController {
    private final TripShareAppService tripShareAppService;

    /** 通过分享 token 查看行程（不需要认证） */
    @GetMapping("/{token}")
    public Result<TripDetailVo> getSharedTrip(@PathVariable String token) {
        return Result.success(tripShareAppService.getSharedTrip(token));
    }
}
