package com.tripdesigner.checkin.application;

import com.tripdesigner.checkin.api.dto.CreateCheckinRequest;
import com.tripdesigner.checkin.api.vo.CheckinStatsVo;
import com.tripdesigner.checkin.api.vo.TripCheckinVo;
import com.tripdesigner.checkin.domain.CheckinStatus;
import com.tripdesigner.checkin.domain.TripCheckin;
import com.tripdesigner.checkin.domain.TripCheckinRepository;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行程签到应用服务。
 *
 * 用例：
 * 1. 创建签到（用户到达景点时记录）
 * 2. 列出用户/行程的签到记录
 * 3. 标记签到状态（完成/跳过）
 * 4. 统计用户打卡情况
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinAppService {

    private final TripCheckinRepository repository;

    @Transactional
    public TripCheckinVo checkin(CreateCheckinRequest req) {
        UserContext ctx = requireAuth();
        // 同一活动不能重复签到
        if (req.getActivityId() != null) {
            repository.findByUserAndActivity(ctx.userId(), req.getActivityId())
                    .ifPresent(c -> {
                        throw new BizException(ResultCode.COMMON_BAD_REQUEST, "该活动已签到");
                    });
        }
        TripCheckin checkin = TripCheckin.create(
                ctx.userId(), req.getTripId(), req.getTripDayId(), req.getActivityId(),
                req.getPlaceName(), req.getLatitude(), req.getLongitude(),
                req.getNotes(), req.getPhotoUrls());
        TripCheckin saved = repository.save(checkin);
        return TripCheckinVo.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TripCheckinVo> listMyCheckins() {
        UserContext ctx = requireAuth();
        return repository.findByUserId(ctx.userId()).stream()
                .map(TripCheckinVo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripCheckinVo> listByTrip(Long tripId) {
        return repository.findByTripId(tripId).stream()
                .map(TripCheckinVo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripCheckinVo getById(Long id) {
        UserContext ctx = requireAuth();
        TripCheckin checkin = loadOwned(id, ctx.userId());
        return TripCheckinVo.from(checkin);
    }

    @Transactional
    public TripCheckinVo updateStatus(Long id, String status) {
        UserContext ctx = requireAuth();
        TripCheckin checkin = loadOwned(id, ctx.userId());
        CheckinStatus newStatus = parseStatus(status);
        TripCheckin updated = repository.save(checkin.withStatus(newStatus));
        return TripCheckinVo.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        UserContext ctx = requireAuth();
        loadOwned(id, ctx.userId());
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CheckinStatsVo getStats() {
        UserContext ctx = requireAuth();
        List<TripCheckin> all = repository.findByUserId(ctx.userId());
        long total = all.size();
        long completed = all.stream().filter(c -> c.getStatus() == CheckinStatus.COMPLETED).count();
        long skipped = all.stream().filter(c -> c.getStatus() == CheckinStatus.SKIPPED).count();
        List<Map<String, Object>> recent = all.stream()
                .limit(5)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("placeName", c.getPlaceName());
                    m.put("status", c.getStatus() != null ? c.getStatus().name() : null);
                    m.put("checkedInAt", c.getCheckedInAt());
                    return m;
                })
                .toList();
        List<String> places = all.stream()
                .map(TripCheckin::getPlaceName)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
        return CheckinStatsVo.builder()
                .userId(ctx.userId())
                .totalCheckins(total)
                .completedCount(completed)
                .skippedCount(skipped)
                .recentCheckins(recent)
                .visitedPlaces(places)
                .build();
    }

    private TripCheckin loadOwned(Long id, Long currentUserId) {
        TripCheckin checkin = repository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "checkin not found"));
        if (!checkin.getUserId().equals(currentUserId)) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "checkin does not belong to user");
        }
        return checkin;
    }

    private CheckinStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "status is required");
        }
        try {
            return CheckinStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "invalid status: " + value);
        }
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
