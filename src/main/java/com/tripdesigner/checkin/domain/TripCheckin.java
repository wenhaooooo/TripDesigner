package com.tripdesigner.checkin.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 行程签到领域实体。
 *
 * 记录用户到达行程中的某个活动地点并完成签到。
 * 支持附注、照片、地理坐标。
 */
@Getter
@Builder
public class TripCheckin {

    private Long id;
    private Long userId;
    private Long tripId;
    private Long tripDayId;
    private Long activityId;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private List<String> photoUrls;
    private CheckinStatus status;
    private Instant checkedInAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static TripCheckin create(Long userId, Long tripId, Long tripDayId, Long activityId,
                                        String placeName, BigDecimal latitude, BigDecimal longitude,
                                        String notes, List<String> photoUrls) {
        return TripCheckin.builder()
                .userId(userId)
                .tripId(tripId)
                .tripDayId(tripDayId)
                .activityId(activityId)
                .placeName(placeName)
                .latitude(latitude)
                .longitude(longitude)
                .notes(notes)
                .photoUrls(photoUrls != null ? photoUrls : List.of())
                .status(CheckinStatus.CHECKED_IN)
                .checkedInAt(Instant.now())
                .version(0)
                .build();
    }

    public TripCheckin withStatus(CheckinStatus newStatus) {
        return TripCheckin.builder()
                .id(id).userId(userId).tripId(tripId).tripDayId(tripDayId).activityId(activityId)
                .placeName(placeName).latitude(latitude).longitude(longitude)
                .notes(notes).photoUrls(photoUrls).status(newStatus)
                .checkedInAt(checkedInAt).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
