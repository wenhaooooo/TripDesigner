package com.tripdesigner.trip.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.ai.trip.TripGenerationTaskRepository;
import com.tripdesigner.checkin.domain.TripCheckinRepository;
import com.tripdesigner.experience.domain.ExperienceRepository;
import com.tripdesigner.multimodal.domain.MultimodalUploadRepository;
import com.tripdesigner.price.domain.PriceMonitorRepository;
import com.tripdesigner.trip.api.dto.CreateTripRequest;
import com.tripdesigner.trip.api.dto.UpdateTripRequest;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.domain.*;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripAppService {
    private final TripRepository tripRepo;
    private final TripDayRepository tripDayRepo;
    private final TripActivityRepository tripActivityRepo;
    private final TripGenerationTaskRepository tripGenerationTaskRepo;
    private final TripShareRepository tripShareRepo;
    private final PriceMonitorRepository priceMonitorRepo;
    private final TripCheckinRepository tripCheckinRepo;
    private final ExperienceRepository experienceRepo;
    private final MultimodalUploadRepository multimodalUploadRepo;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TRIP_DETAIL_CACHE_KEY = "trip:detail:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Transactional(readOnly = true)
    public List<TripVo> list() {
        UserContext ctx = requireAuth();
        return tripRepo.findByUserId(ctx.userId()).stream()
                .map(TripVo::from)
                .collect(Collectors.toList());
    }

    /** 分页查询当前用户的行程列表 */
    @Transactional(readOnly = true)
    public PageResult<TripVo> list(int page, int size) {
        UserContext ctx = requireAuth();
        List<TripVo> records = tripRepo.findByUserId(ctx.userId(), page, size).stream()
                .map(TripVo::from)
                .collect(Collectors.toList());
        long total = tripRepo.countByUserId(ctx.userId());
        return new PageResult<>(page, size, total, records);
    }

    /** 按关键词搜索当前用户的行程 */
    @Transactional(readOnly = true)
    public PageResult<TripVo> search(String keyword, int page, int size) {
        UserContext ctx = requireAuth();
        List<TripVo> records = tripRepo.searchByUserId(ctx.userId(), keyword, page, size).stream()
                .map(TripVo::from)
                .collect(Collectors.toList());
        long total = tripRepo.countByUserIdAndKeyword(ctx.userId(), keyword);
        return new PageResult<>(page, size, total, records);
    }

    /** Agent-friendly: list trips for a specific user (no auth check) */
    @Transactional(readOnly = true)
    public List<TripVo> listForUser(Long userId) {
        return tripRepo.findByUserId(userId).stream()
                .map(TripVo::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TripVo create(CreateTripRequest req) {
        UserContext ctx = requireAuth();
        Trip trip = Trip.create(
                ctx.userId(),
                req.getTitle(),
                req.getDescription(),
                req.getDestinationName(),
                req.getStartDate(),
                req.getEndDate(),
                req.getBudget()
        );
        return TripVo.from(tripRepo.save(trip));
    }

    @Transactional(readOnly = true)
    public TripDetailVo get(Long tripId) {
        UserContext ctx = requireAuth();
        // 尝试从缓存读取
        String cacheKey = TRIP_DETAIL_CACHE_KEY + tripId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                TripDetailVo cachedVo = objectMapper.readValue(cached, TripDetailVo.class);
                // 验证缓存数据的所属用户
                if (cachedVo != null && cachedVo.getUserId().equals(ctx.userId())) {
                    return cachedVo;
                }
            }
        } catch (Exception e) {
            log.warn("[TripAppService] Cache read failed for trip {}: {}", tripId, e.getMessage());
        }

        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), ctx.userId());

        List<TripDayVo> days = tripDayRepo.findByTripId(tripId).stream()
                .map(d -> TripDayVo.from(d, tripActivityRepo.findByTripDayId(d.getId())
                        .stream().map(TripActivityVo::from).collect(Collectors.toList())))
                .collect(Collectors.toList());

        TripDetailVo result = TripDetailVo.from(TripVo.from(trip), days);

        // 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("[TripAppService] Cache write failed for trip {}: {}", tripId, e.getMessage());
        }

        return result;
    }

    @Transactional
    public TripVo update(Long tripId, UpdateTripRequest req) {
        UserContext ctx = requireAuth();
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), ctx.userId());

        Trip updated = trip.withUpdatedFields(
                req.getTitle(), req.getDescription(), req.getDestinationName(),
                req.getStartDate(), req.getEndDate(), req.getBudget());
        TripVo result = TripVo.from(tripRepo.save(updated));
        evictCache(tripId);
        return result;
    }

    @Transactional
    public void delete(Long tripId) {
        UserContext ctx = requireAuth();
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), ctx.userId());

        tripShareRepo.findByTripId(tripId).forEach(s -> tripShareRepo.deleteById(s.getId()));

        priceMonitorRepo.findByTripId(tripId).forEach(m -> priceMonitorRepo.deleteById(m.getId()));

        tripCheckinRepo.findByTripId(tripId).forEach(c -> tripCheckinRepo.deleteById(c.getId()));

        experienceRepo.findByTripId(tripId).forEach(e -> experienceRepo.delete(e.getId()));

        multimodalUploadRepo.findByGeneratedTripId(tripId).forEach(u -> multimodalUploadRepo.deleteById(u.getId()));

        tripGenerationTaskRepo.findByTripId(tripId).forEach(t -> tripGenerationTaskRepo.deleteById(t.getId()));

        List<TripDay> days = tripDayRepo.findByTripId(tripId);
        for (TripDay day : days) {
            List<TripActivity> activities = tripActivityRepo.findByTripDayId(day.getId());
            for (TripActivity activity : activities) {
                tripActivityRepo.deleteById(activity.getId());
            }
            tripDayRepo.deleteById(day.getId());
        }

        tripRepo.deleteById(tripId);
        evictCache(tripId);
    }

    @Transactional
    public TripVo updateStatus(Long tripId, TripStatus status) {
        UserContext ctx = requireAuth();
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), ctx.userId());

        Trip updated = trip.withUpdatedStatus(status);
        TripVo result = TripVo.from(tripRepo.save(updated));
        evictCache(tripId);
        return result;
    }

    private void evictCache(Long tripId) {
        try {
            redisTemplate.delete(TRIP_DETAIL_CACHE_KEY + tripId);
        } catch (Exception e) {
            log.warn("[TripAppService] Cache evict failed for trip {}: {}", tripId, e.getMessage());
        }
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private void verifyOwner(Long ownerId, Long currentUserId) {
        if (!ownerId.equals(currentUserId)) {
            throw new BizException(ResultCode.TRIP_NOT_OWNER);
        }
    }

    // ========== Agent-friendly methods (explicit userId, no auth check) ==========

    @Transactional
    public TripVo createForUser(Long userId, String title, String description, String destinationName,
                                 LocalDate startDate, LocalDate endDate, Integer budget) {
        Trip trip = Trip.create(userId, title, description, destinationName, startDate, endDate, budget);
        return TripVo.from(tripRepo.save(trip));
    }

    @Transactional(readOnly = true)
    public TripDetailVo getDetailForUser(Long tripId, Long userId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), userId);

        List<TripDayVo> days = tripDayRepo.findByTripId(tripId).stream()
                .map(d -> TripDayVo.from(d, tripActivityRepo.findByTripDayId(d.getId())
                        .stream().map(TripActivityVo::from).collect(Collectors.toList())))
                .collect(Collectors.toList());

        return TripDetailVo.from(TripVo.from(trip), days);
    }

    @Transactional
    public TripDayVo addDayForTrip(Long tripId, Long userId, Integer dayNumber, LocalDate date,
                                    String title, String description) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), userId);

        TripDay day = TripDay.create(tripId, dayNumber, date, title, description);
        TripDay saved = tripDayRepo.save(day);
        return TripDayVo.from(saved, List.of());
    }

    @Transactional
    public TripDayVo findOrCreateDayForTrip(Long tripId, Long userId, Integer dayNumber) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), userId);

        List<TripDay> existingDays = tripDayRepo.findByTripId(tripId);
        for (TripDay day : existingDays) {
            if (day.getDayNumber().equals(dayNumber)) {
                return TripDayVo.from(day, List.of());
            }
        }

        LocalDate date = trip.getStartDate().plusDays(dayNumber - 1);
        String title = "第" + dayNumber + "天";
        return addDayForTrip(tripId, userId, dayNumber, date, title, null);
    }

    @Transactional
    public TripActivityVo addActivityForDay(Long tripDayId, Long userId, String name,
                                              LocalTime startTime, LocalTime endTime, String category,
                                              String place, String description) {
        TripDay day = tripDayRepo.findById(tripDayId).orElse(null);
        
        if (day == null) {
            log.warn("[TripAppService] Trip day {} not found", tripDayId);
            throw new BizException(ResultCode.TRIP_DAY_NOT_FOUND, 
                    "行程天数不存在，请先创建行程天数");
        }
        
        Trip trip = tripRepo.findById(day.getTripId())
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), userId);

        TripActivity activity = TripActivity.create(tripDayId, name, startTime, endTime, category, place, description);
        TripActivity saved = tripActivityRepo.save(activity);
        return TripActivityVo.from(saved);
    }

    @Transactional
    public TripActivityVo addActivityForDayByNumber(Long tripId, Long userId, Integer dayNumber,
                                                     String name, LocalTime startTime, LocalTime endTime, 
                                                     String category, String place, String description) {
        TripDayVo dayVo = findOrCreateDayForTrip(tripId, userId, dayNumber);
        return addActivityForDay(dayVo.getId(), userId, name, startTime, endTime, category, place, description);
    }
}
