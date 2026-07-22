package com.tripdesigner.experience.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Domain entity for user-generated travel experiences tied to trips, days, or activities.
 */
@Getter
@Builder
public class TripExperience {
    private Long id;
    private Long userId;
    private Long tripId;
    private Long tripDayId;
    private Long tripActivityId;
    private String title;
    private String content;
    private Integer rating;
    private List<String> tags;
    private List<String> mediaUrls;
    private ExperienceStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static TripExperience create(Long userId, Long tripId, String title, String content,
                                         Integer rating, List<String> tags, List<String> mediaUrls) {
        return TripExperience.builder()
                .userId(userId)
                .tripId(tripId)
                .title(title)
                .content(content)
                .rating(rating)
                .tags(tags)
                .mediaUrls(mediaUrls != null ? mediaUrls : List.of())
                .status(ExperienceStatus.PUBLISHED)
                .version(0)
                .build();
    }

    public TripExperience withUpdatedFields(String title, String content, Integer rating,
                                             List<String> tags, List<String> mediaUrls) {
        return TripExperience.builder()
                .id(id).userId(userId).tripId(tripId).tripDayId(tripDayId).tripActivityId(tripActivityId)
                .title(title != null ? title : this.title)
                .content(content != null ? content : this.content)
                .rating(rating != null ? rating : this.rating)
                .tags(tags != null ? tags : this.tags)
                .mediaUrls(mediaUrls != null ? mediaUrls : this.mediaUrls)
                .status(status)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public TripExperience withDay(Long tripDayId) {
        return TripExperience.builder()
                .id(id).userId(userId).tripId(tripId).tripDayId(tripDayId).tripActivityId(tripActivityId)
                .title(title).content(content).rating(rating).tags(tags).mediaUrls(mediaUrls)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public TripExperience withActivity(Long tripActivityId) {
        return TripExperience.builder()
                .id(id).userId(userId).tripId(tripId).tripDayId(tripDayId).tripActivityId(tripActivityId)
                .title(title).content(content).rating(rating).tags(tags).mediaUrls(mediaUrls)
                .status(status).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
