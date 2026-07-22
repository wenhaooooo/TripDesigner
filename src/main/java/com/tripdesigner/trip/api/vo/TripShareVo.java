package com.tripdesigner.trip.api.vo;
/**
 * 行程分享视图对象（VO）。
 * 用于分享链接列表展示，不包含关联的行程详情。
 */

import com.tripdesigner.trip.domain.TripShare;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TripShareVo {
    private final Long id;
    private final Long tripId;
    private final String shareToken;
    private final String shareType;
    private final Integer maxViews;
    private final Integer currentViews;
    private final Instant expiresAt;
    private final String status;
    private final Instant createdAt;

    public static TripShareVo from(TripShare s) {
        return TripShareVo.builder()
                .id(s.getId())
                .tripId(s.getTripId())
                .shareToken(s.getShareToken())
                .shareType(s.getShareType() != null ? s.getShareType().name() : null)
                .maxViews(s.getMaxViews())
                .currentViews(s.getCurrentViews())
                .expiresAt(s.getExpiresAt())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
