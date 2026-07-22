package com.tripdesigner.trip.domain;
/**
 * 目的地领域实体。
 * 代表一个可供选择的目的地，包含国家和区域分类。
 */

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Destination {
    private Long id;
    private String name;
    private String country;
    private String region;
    private String category;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static Destination create(String name, String country, String region,
                                     String category, String description) {
        return Destination.builder()
                .name(name)
                .country(country)
                .region(region)
                .category(category)
                .description(description)
                .version(0)
                .build();
    }

    public Destination withUpdatedFields(String name, String country, String region,
                                          String category, String description) {
        return Destination.builder()
                .id(id)
                .name(name != null ? name : this.name)
                .country(country != null ? country : this.country)
                .region(region != null ? region : this.region)
                .category(category != null ? category : this.category)
                .description(description != null ? description : this.description)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
