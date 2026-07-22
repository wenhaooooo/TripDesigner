package com.tripdesigner.trip.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripShareVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.domain.ShareStatus;
import com.tripdesigner.trip.domain.ShareType;
import com.tripdesigner.trip.domain.Trip;
import com.tripdesigner.trip.domain.TripActivityRepository;
import com.tripdesigner.trip.domain.TripDayRepository;
import com.tripdesigner.trip.domain.TripRepository;
import com.tripdesigner.trip.domain.TripShare;
import com.tripdesigner.trip.domain.TripShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 行程分享应用服务。
 *
 * 负责行程分享链接的创建、查询、撤销，以及通过分享 token 公开访问行程详情。
 * - 创建/列出/撤销分享需要认证且校验行程所有权
 * - 通过 token 访问行程不需要认证，但会增加访问次数并校验分享状态
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripShareAppService {
    private final TripShareRepository tripShareRepo;
    private final TripRepository tripRepo;
    private final TripDayRepository tripDayRepo;
    private final TripActivityRepository tripActivityRepo;

    /**
     * 创建行程分享链接。
     *
     * @param tripId     行程 ID
     * @param type       分享类型（VIEW/EDIT）
     * @param maxViews   最大访问次数（null 表示不限制）
     * @param expireDays 过期天数（null 表示不过期）
     * @return 分享链接 VO
     */
    @Transactional
    public TripShareVo createShare(Long tripId, ShareType type, Integer maxViews, Integer expireDays) {
        UserContext ctx = requireAuth();
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), ctx.userId());

        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = expireDays != null ? Instant.now().plus(Duration.ofDays(expireDays)) : null;

        TripShare share = TripShare.create(tripId, ctx.userId(), token, type, maxViews, expiresAt);
        return TripShareVo.from(tripShareRepo.save(share));
    }

    /**
     * 通过分享 token 获取行程详情（不需要认证）。
     * 会校验分享是否可访问，并增加访问次数。
     *
     * @param token 分享 token
     * @return 行程详情 VO
     */
    @Transactional
    public TripDetailVo getSharedTrip(String token) {
        TripShare share = tripShareRepo.findByToken(token)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "share not found"));

        if (!share.canAccess()) {
            if (share.isRevoked()) {
                throw new BizException(ResultCode.PERMISSION_DENIED, "share revoked");
            }
            throw new BizException(ResultCode.PERMISSION_DENIED, "share expired");
        }

        TripShare incremented = share.incrementViews();
        tripShareRepo.save(incremented);

        Trip trip = tripRepo.findById(share.getTripId())
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));

        List<TripDayVo> days = tripDayRepo.findByTripId(share.getTripId()).stream()
                .map(d -> TripDayVo.from(d, tripActivityRepo.findByTripDayId(d.getId())
                        .stream().map(TripActivityVo::from).collect(Collectors.toList())))
                .collect(Collectors.toList());

        return TripDetailVo.from(TripVo.from(trip), days);
    }

    /**
     * 撤销分享链接。
     *
     * @param shareId 分享 ID
     */
    @Transactional
    public void revokeShare(Long shareId) {
        UserContext ctx = requireAuth();
        TripShare share = tripShareRepo.findById(shareId)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "share not found"));
        verifyOwner(share.getOwnerUserId(), ctx.userId());

        TripShare revoked = share.withUpdatedStatus(ShareStatus.REVOKED);
        tripShareRepo.save(revoked);
    }

    /**
     * 列出行程的所有分享链接。
     *
     * @param tripId 行程 ID
     * @return 分享链接 VO 列表
     */
    @Transactional(readOnly = true)
    public List<TripShareVo> listSharesByTrip(Long tripId) {
        UserContext ctx = requireAuth();
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BizException(ResultCode.TRIP_NOT_FOUND));
        verifyOwner(trip.getUserId(), ctx.userId());

        return tripShareRepo.findByTripId(tripId).stream()
                .map(TripShareVo::from)
                .collect(Collectors.toList());
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
}
