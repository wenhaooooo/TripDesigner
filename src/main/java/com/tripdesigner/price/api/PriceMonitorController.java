package com.tripdesigner.price.api;

import com.tripdesigner.common.response.Result;
import com.tripdesigner.price.api.dto.CreateMonitorRequest;
import com.tripdesigner.price.api.vo.PriceMonitorVo;
import com.tripdesigner.price.api.vo.TrainTicketInfoVo;
import com.tripdesigner.price.application.PriceMonitorAppService;
import com.tripdesigner.price.application.PriceMonitorService;
import com.tripdesigner.price.domain.TicketClass;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 价格监测 REST API 控制器。
 *
 * 提供价格监测的创建、查询、取消和删除操作。
 * 所有操作需认证，且只能操作当前用户的监测。
 */
@RestController
@RequestMapping("/price-monitors")
@RequiredArgsConstructor
public class PriceMonitorController {

    private final PriceMonitorAppService priceMonitorAppService;
    private final PriceMonitorService priceMonitorService;

    /** 列出当前用户的价格监测 */
    @GetMapping
    public Result<List<PriceMonitorVo>> list() {
        return Result.success(priceMonitorAppService.listMyMonitors());
    }

    /** 获取价格监测详情 */
    @GetMapping("/{id}")
    public Result<PriceMonitorVo> get(@PathVariable Long id) {
        return Result.success(priceMonitorAppService.getMonitor(id));
    }

    /** 创建价格监测 */
    @PostMapping
    public Result<PriceMonitorVo> create(@Valid @RequestBody CreateMonitorRequest req) {
        return Result.success(priceMonitorAppService.createMonitor(req));
    }

    /** 取消价格监测（状态置为 CANCELLED） */
    @PutMapping("/{id}/cancel")
    public Result<PriceMonitorVo> cancel(@PathVariable Long id) {
        return Result.success(priceMonitorAppService.cancelMonitor(id));
    }

    /** 删除价格监测 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        priceMonitorAppService.deleteMonitor(id);
        return Result.success();
    }

    /** 查询可用车次信息 */
    @GetMapping("/trains")
    public Result<List<TrainTicketInfoVo>> listTrains(
            @RequestParam String departure,
            @RequestParam String destination,
            @RequestParam(required = false) String ticketClass) {
        TicketClass tc = ticketClass != null && !ticketClass.isBlank()
                ? TicketClass.valueOf(ticketClass.toUpperCase())
                : null;
        List<TrainTicketInfoVo> result = priceMonitorService.findTrainTickets(departure, destination, tc)
                .stream()
                .map(TrainTicketInfoVo::from)
                .toList();
        return Result.success(result);
    }
}
