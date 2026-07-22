package com.tripdesigner.trip.api;
/**
 * 目的地 REST API 控制器。
 * 提供目的地的 CRUD 和按国家/分类查询操作。
 * 目的地作为独立资源管理。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.trip.api.dto.CreateDestinationRequest;
import com.tripdesigner.trip.api.dto.UpdateDestinationRequest;
import com.tripdesigner.trip.api.vo.DestinationVo;
import com.tripdesigner.trip.application.DestinationAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/destinations")
@RequiredArgsConstructor
public class DestinationController {
    private final DestinationAppService destAppService;

    @GetMapping
    public Result<List<DestinationVo>> list(@RequestParam(required = false) String country) {
        List<DestinationVo> list;
        if (country != null && !country.isBlank()) {
            list = destAppService.listByCountry(country);
        } else {
            list = destAppService.list();
        }
        return Result.success(list);
    }

    @PostMapping
    public Result<DestinationVo> create(@Valid @RequestBody CreateDestinationRequest req) {
        return Result.success(destAppService.create(req));
    }

    @GetMapping("/{id}")
    public Result<DestinationVo> get(@PathVariable Long id) {
        return Result.success(destAppService.get(id));
    }

    @PutMapping("/{id}")
    public Result<DestinationVo> update(@PathVariable Long id, @Valid @RequestBody UpdateDestinationRequest req) {
        return Result.success(destAppService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        destAppService.delete(id);
        return Result.success();
    }
}
