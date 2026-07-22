package com.tripdesigner.booking.api;

import com.tripdesigner.booking.api.vo.BookingLinkVo;
import com.tripdesigner.booking.application.BookingService;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 在线预订 REST API。
 *
 * - GET /booking/trips/{tripId}/activities/{activityId}  生成单个活动的预订链接
 * - GET /booking/trips/{tripId}/suggestions                生成整个行程的预订建议（按天分组）
 */
@Tag(name = "Booking", description = "在线预订链接生成")
@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "生成活动预订链接")
    @GetMapping("/trips/{tripId}/activities/{activityId}")
    public Result<List<BookingLinkVo>> getLinks(@PathVariable Long tripId,
                                                  @PathVariable Long activityId) {
        UserContext ctx = requireAuth();
        return Result.success(bookingService.generateLinksForActivity(ctx.userId(), tripId, activityId));
    }

    @Operation(summary = "生成行程预订建议（按天分组）")
    @GetMapping("/trips/{tripId}/suggestions")
    public Result<Map<String, List<BookingLinkVo.Suggestion>>> getSuggestions(@PathVariable Long tripId) {
        UserContext ctx = requireAuth();
        return Result.success(bookingService.generateSuggestionsForTrip(ctx.userId(), tripId));
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
