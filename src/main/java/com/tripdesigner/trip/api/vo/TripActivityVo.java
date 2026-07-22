package com.tripdesigner.trip.api.vo;
/**
 * 活动视图对象（VO）。
 */

import com.tripdesigner.trip.domain.TripActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripActivityVo {
    private Long id;
    private String name;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private String category;
    private String place;
    private String notes;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public static TripActivityVo from(TripActivity a) {
        return TripActivityVo.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .category(a.getCategory())
                .place(a.getPlace())
                .notes(a.getNotes())
                .sortOrder(a.getSortOrder())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
