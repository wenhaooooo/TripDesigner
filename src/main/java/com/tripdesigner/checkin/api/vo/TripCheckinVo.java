package com.tripdesigner.checkin.api.vo;

import com.tripdesigner.checkin.domain.CheckinStatus;
import com.tripdesigner.checkin.domain.TripCheckin;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class TripCheckinVo {
    private final Long id;
    private final Long userId;
    private final Long tripId;
    private final Long tripDayId;
    private final Long activityId;
    private final String placeName;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String notes;
    private final List<String> photoUrls;
    private final String status;
    private final Instant checkedInAt;
    private final Instant createdAt;

    public static TripCheckinVo from(TripCheckin c) {
        return TripCheckinVo.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .tripId(c.getTripId())
                .tripDayId(c.getTripDayId())
                .activityId(c.getActivityId())
                .placeName(c.getPlaceName())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .notes(c.getNotes())
                .photoUrls(c.getPhotoUrls())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .checkedInAt(c.getCheckedInAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
