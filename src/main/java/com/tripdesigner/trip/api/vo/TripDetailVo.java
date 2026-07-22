package com.tripdesigner.trip.api.vo;
/**
 * 行程详情视图对象（VO）。
 * 包含行程基本信息 + 所有行程日及活动的完整树形结构。
 * 扁平化结构，直接包含 TripVo 的所有字段。
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDetailVo {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String destinationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer budget;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TripDayVo> days;

    public static TripDetailVo from(TripVo trip, List<TripDayVo> days) {
        return TripDetailVo.builder()
                .id(trip.getId())
                .userId(trip.getUserId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .destinationName(trip.getDestinationName())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .budget(trip.getBudget())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .days(days)
                .build();
    }
}
