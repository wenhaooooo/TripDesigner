package com.tripdesigner.trip.api.vo;
/**
 * 行程视图对象（VO）。
 * 用于行程列表展示，不包含行程日和活动的嵌套数据。
 */

import com.tripdesigner.trip.domain.Trip;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class TripVo {
    private final Long id;
    private final Long userId;
    private final String title;
    private final String description;
    private final String destinationName;
    private final String status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer budget;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static TripVo from(Trip t) {
        return TripVo.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .title(t.getTitle())
                .description(t.getDescription())
                .destinationName(t.getDestinationName())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .budget(t.getBudget())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
