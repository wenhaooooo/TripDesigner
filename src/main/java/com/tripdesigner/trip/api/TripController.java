package com.tripdesigner.trip.api;
/**
 * 行程 REST API 控制器。
 * 提供行程的 CRUD 操作，包含状态更新和详情查询。
 * 所有操作需认证，且只能操作当前用户的行程。
 */

import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.trip.api.dto.CreateTripRequest;
import com.tripdesigner.trip.api.dto.UpdateTripRequest;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.application.TripAppService;
import com.tripdesigner.trip.domain.TripStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripAppService tripAppService;

    @GetMapping
    public Result<List<TripVo>> list() {
        return Result.success(tripAppService.list());
    }

    /** 分页查询行程列表 */
    @GetMapping("/page")
    public Result<PageResult<TripVo>> listPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(tripAppService.list(page, size));
    }

    /** 按关键词搜索行程 */
    @GetMapping("/search")
    public Result<PageResult<TripVo>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(tripAppService.search(keyword, page, size));
    }

    @PostMapping
    public Result<TripVo> create(@Valid @RequestBody CreateTripRequest req) {
        return Result.success(tripAppService.create(req));
    }

    @GetMapping("/{id}")
    public Result<TripDetailVo> get(@PathVariable Long id) {
        return Result.success(tripAppService.get(id));
    }

    @PutMapping("/{id}")
    public Result<TripVo> update(@PathVariable Long id, @Valid @RequestBody UpdateTripRequest req) {
        return Result.success(tripAppService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tripAppService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<TripVo> updateStatus(@PathVariable Long id, @RequestParam TripStatus status) {
        return Result.success(tripAppService.updateStatus(id, status));
    }
}
