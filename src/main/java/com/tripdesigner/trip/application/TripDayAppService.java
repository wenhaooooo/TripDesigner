package com.tripdesigner.trip.application;
/**
 * 行程日应用服务。
 * 处理行程日的业务逻辑，包含权限校验。
 * 添加活动时自动校验行程日归属的行程是否属于当前用户。
 */

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.dto.CreateTripDayRequest;
import com.tripdesigner.trip.api.dto.UpdateTripDayRequest;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripDayAppService {
    private final TripRepository tripRepo;
    private final TripDayRepository tripDayRepo;

    @Transactional(readOnly = true)
    public List<TripDayVo> list(Long tripId) {
        UserContext ctx = requireAuth();
        verifyTripOwner(tripId, ctx.userId());
        return tripDayRepo.findByTripId(tripId).stream()
                .map(d -> TripDayVo.from(d, List.of()))
                .collect(Collectors.toList());
    }

    @Transactional
    public TripDayVo create(Long tripId, CreateTripDayRequest req) {
        UserContext ctx = requireAuth();
        verifyTripOwner(tripId, ctx.userId());
        TripDay day = TripDay.create(tripId, req.getDayNumber(), req.getDate(),
                req.getTitle(), req.getDescription());
        TripDay saved = tripDayRepo.save(day);
        return TripDayVo.from(saved, List.of());
    }

    @Transactional
    public TripDayVo update(Long tripId, Long dayId, UpdateTripDayRequest req) {
        UserContext ctx = requireAuth();
        verifyTripOwner(tripId, ctx.userId());

        TripDay day = tripDayRepo.findById(dayId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_DAY_NOT_FOUND));
        if (!day.getTripId().equals(tripId)) {
            throw new BizException(ResultCode.TRIP_DAY_NOT_FOUND);
        }

        TripDay updated = day.withUpdatedFields(req.getDate(), req.getTitle(), req.getDescription());
        TripDay saved = tripDayRepo.save(updated);
        return TripDayVo.from(saved, List.of());
    }

    @Transactional
    public void delete(Long tripId, Long dayId) {
        UserContext ctx = requireAuth();
        verifyTripOwner(tripId, ctx.userId());

        TripDay day = tripDayRepo.findById(dayId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_DAY_NOT_FOUND));
        if (!day.getTripId().equals(tripId)) {
            throw new BizException(ResultCode.TRIP_DAY_NOT_FOUND);
        }
        tripDayRepo.deleteById(dayId);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private void verifyTripOwner(Long tripId, Long userId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        if (!trip.getUserId().equals(userId)) {
            throw new BizException(ResultCode.TRIP_NOT_OWNER);
        }
    }
}
