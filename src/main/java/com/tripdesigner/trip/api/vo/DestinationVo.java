package com.tripdesigner.trip.api.vo;
/**
 * 目的地视图对象（VO）。
 */

import com.tripdesigner.trip.domain.Destination;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DestinationVo {
    private final Long id;
    private final String name;
    private final String country;
    private final String region;
    private final String category;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static DestinationVo from(Destination d) {
        return DestinationVo.builder()
                .id(d.getId())
                .name(d.getName())
                .country(d.getCountry())
                .region(d.getRegion())
                .category(d.getCategory())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
