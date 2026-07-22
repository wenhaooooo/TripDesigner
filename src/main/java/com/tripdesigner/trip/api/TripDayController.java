package com.tripdesigner.trip.api;
/**
 * 行程日 REST API 控制器。
 * 提供对指定行程下行程日的 CRUD 操作。
 * 必须先有 Trip，再添加 TripDay。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.trip.api.dto.CreateTripDayRequest;
import com.tripdesigner.trip.api.dto.UpdateTripDayRequest;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.application.TripDayAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips/{tripId}/days")
@RequiredArgsConstructor
public class TripDayController {
    private final TripDayAppService tripDayAppService;

    @GetMapping
    public Result<List<TripDayVo>> list(@PathVariable Long tripId) {
        return Result.success(tripDayAppService.list(tripId));
    }

    @PostMapping
    public Result<TripDayVo> create(@PathVariable Long tripId, @Valid @RequestBody CreateTripDayRequest req) {
        return Result.success(tripDayAppService.create(tripId, req));
    }

    @PutMapping("/{dayId}")
    public Result<TripDayVo> update(@PathVariable Long tripId, @PathVariable Long dayId,
                                     @Valid @RequestBody UpdateTripDayRequest req) {
        return Result.success(tripDayAppService.update(tripId, dayId, req));
    }

    @DeleteMapping("/{dayId}")
    public Result<Void> delete(@PathVariable Long tripId, @PathVariable Long dayId) {
        tripDayAppService.delete(tripId, dayId);
        return Result.success();
    }
}
