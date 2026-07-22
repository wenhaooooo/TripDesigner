package com.tripdesigner.statistics.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.statistics.api.vo.TripStatisticsVo;
import com.tripdesigner.statistics.application.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅行统计 REST API。
 *
 * - GET /statistics  仪表盘数据（行程概览、目的地分布、活动类别、月度统计、成就）
 */
@Tag(name = "Statistics", description = "旅行统计仪表盘")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "获取旅行统计数据")
    @GetMapping
    public Result<TripStatisticsVo> getStatistics() {
        requireAuth();
        return Result.success(statisticsService.getStatistics());
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
