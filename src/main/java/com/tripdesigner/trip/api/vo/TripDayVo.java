package com.tripdesigner.trip.api.vo;
/**
 * 行程日视图对象（VO）。
 * 包含行程日信息 + 该日的所有活动列表。
 */

import com.tripdesigner.trip.domain.TripDay;
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
public class TripDayVo {
    private Long id;
    private Integer dayNumber;
    private LocalDate date;
    private String title;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TripActivityVo> activities;

    public static TripDayVo from(TripDay d, List<TripActivityVo> activities) {
        return TripDayVo.builder()
                .id(d.getId())
                .dayNumber(d.getDayNumber())
                .date(d.getDate())
                .title(d.getTitle())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .activities(activities)
                .build();
    }
}
