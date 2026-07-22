package com.tripdesigner.memory.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * 旅行记忆领域实体。
 * 从过往行程中提炼的洞察和经验，用于未来行程的个性化推荐。
 * 类型包括：PREFERENCE_DISCOVERED, LESSON_LEARNED, HIGHLIGHT, LOWLIGHT, ADVICE
 */
@Getter
@Builder
public class TripMemory {
    private Long id;
    private Long userId;
    private Long tripId;
    private String memoryType;
    private String content;
    private List<String> tags;
    private Instant createdAt;

    public static TripMemory create(Long userId, Long tripId, String memoryType,
                                    String content, List<String> tags) {
        return TripMemory.builder()
                .userId(userId)
                .tripId(tripId)
                .memoryType(memoryType)
                .content(content)
                .tags(tags != null ? tags : List.of())
                .build();
    }
}