package com.tripdesigner.trip.application;
/**
 * 活动应用服务。
 * 处理活动的业务逻辑，包含权限校验（操作者必须是行程所有者）。
 */

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.dto.CreateTripActivityRequest;
import com.tripdesigner.trip.api.dto.UpdateTripActivityRequest;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripActivityAppService {
    private final TripRepository tripRepo;
    private final TripDayRepository tripDayRepo;
    private final TripActivityRepository tripActivityRepo;

    @Transactional(readOnly = true)
    public List<TripActivityVo> list(Long tripId, Long dayId) {
        UserContext ctx = requireAuth();
        verifyDayOwner(tripId, dayId, ctx.userId());
        return tripActivityRepo.findByTripDayId(dayId).stream()
                .map(TripActivityVo::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TripActivityVo create(Long tripId, Long dayId, CreateTripActivityRequest req) {
        UserContext ctx = requireAuth();
        verifyDayOwner(tripId, dayId, ctx.userId());

        TripActivity activity = TripActivity.create(dayId, req.getName(), req.getStartTime(),
                req.getEndTime(), req.getCategory(), req.getPlace(), req.getNotes());
        TripActivity saved = tripActivityRepo.save(activity);
        return TripActivityVo.from(saved);
    }

    @Transactional
    public TripActivityVo update(Long tripId, Long dayId, Long activityId, UpdateTripActivityRequest req) {
        UserContext ctx = requireAuth();
        verifyDayOwner(tripId, dayId, ctx.userId());

        TripActivity activity = tripActivityRepo.findById(activityId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_ACTIVITY_NOT_FOUND));
        if (!activity.getTripDayId().equals(dayId)) {
            throw new BizException(ResultCode.TRIP_ACTIVITY_NOT_FOUND);
        }

        TripActivity updated = activity.withUpdatedFields(req.getName(), req.getStartTime(),
                req.getEndTime(), req.getCategory(), req.getPlace(), req.getNotes(), req.getSortOrder());
        TripActivity saved = tripActivityRepo.save(updated);
        return TripActivityVo.from(saved);
    }

    @Transactional
    public void delete(Long tripId, Long dayId, Long activityId) {
        UserContext ctx = requireAuth();
        verifyDayOwner(tripId, dayId, ctx.userId());

        TripActivity activity = tripActivityRepo.findById(activityId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_ACTIVITY_NOT_FOUND));
        if (!activity.getTripDayId().equals(dayId)) {
            throw new BizException(ResultCode.TRIP_ACTIVITY_NOT_FOUND);
        }
        tripActivityRepo.deleteById(activityId);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private void verifyDayOwner(Long tripId, Long dayId, Long userId) {
        TripDay day = tripDayRepo.findById(dayId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_DAY_NOT_FOUND));
        if (!day.getTripId().equals(tripId)) {
            throw new BizException(ResultCode.TRIP_DAY_NOT_FOUND);
        }
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        if (!trip.getUserId().equals(userId)) {
            throw new BizException(ResultCode.TRIP_NOT_OWNER);
        }
    }
}
