package com.tripdesigner.memory.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 用户偏好领域实体。
 * 存储用户的个性化偏好信息，用于 AI 个性化推荐。
 * data 字段使用 Map 存储键值对形式的偏好数据。
 */
@Getter
@Builder
public class UserPreference {
    private Long id;
    private Long userId;
    private String category;
    private Map<String, Object> data;
    private PreferenceSource source;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserPreference create(Long userId, String category,
                                        Map<String, Object> data, PreferenceSource source) {
        return UserPreference.builder()
                .userId(userId)
                .category(category)
                .data(data)
                .source(source)
                .build();
    }
}