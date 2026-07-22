package com.tripdesigner.trip.api;
/**
 * 活动 REST API 控制器。
 * 提供对指定行程日下活动的 CRUD 操作。
 * 必须先有 TripDay，再添加 TripActivity。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.trip.api.dto.CreateTripActivityRequest;
import com.tripdesigner.trip.api.dto.UpdateTripActivityRequest;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.application.TripActivityAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips/{tripId}/days/{dayId}/activities")
@RequiredArgsConstructor
public class TripActivityController {
    private final TripActivityAppService tripActivityAppService;

    @GetMapping
    public Result<List<TripActivityVo>> list(@PathVariable Long tripId, @PathVariable Long dayId) {
        return Result.success(tripActivityAppService.list(tripId, dayId));
    }

    @PostMapping
    public Result<TripActivityVo> create(@PathVariable Long tripId, @PathVariable Long dayId,
                                          @Valid @RequestBody CreateTripActivityRequest req) {
        return Result.success(tripActivityAppService.create(tripId, dayId, req));
    }

    @PutMapping("/{activityId}")
    public Result<TripActivityVo> update(@PathVariable Long tripId, @PathVariable Long dayId,
                                          @PathVariable Long activityId,
                                          @Valid @RequestBody UpdateTripActivityRequest req) {
        return Result.success(tripActivityAppService.update(tripId, dayId, activityId, req));
    }

    @DeleteMapping("/{activityId}")
    public Result<Void> delete(@PathVariable Long tripId, @PathVariable Long dayId,
                                @PathVariable Long activityId) {
        tripActivityAppService.delete(tripId, dayId, activityId);
        return Result.success();
    }
}
