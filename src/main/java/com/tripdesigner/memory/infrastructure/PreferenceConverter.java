package com.tripdesigner.memory.infrastructure;
/**
 * 偏好转换器。
 * 负责 UserPreference 领域实体和 UserPreferencePO 之间的转换。
 * 处理 data 字段的 JSON 序列化/反序列化。
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.memory.domain.PreferenceSource;
import com.tripdesigner.memory.domain.UserPreference;

import java.util.List;
import java.util.Map;

public class PreferenceConverter {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static UserPreference toDomain(UserPreferencePO po) {
        if (po == null) return null;
        return UserPreference.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .category(po.getCategory())
                .data(parseMap(po.getData()))
                .source(mapSource(po.getSource()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static UserPreferencePO toPO(UserPreference domain) {
        if (domain == null) return null;
        UserPreferencePO po = new UserPreferencePO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setCategory(domain.getCategory());
        po.setData(serializeMap(domain.getData()));
        po.setSource(domain.getSource().name());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private static PreferenceSource mapSource(String source) {
        try {
            return PreferenceSource.valueOf(source);
        } catch (Exception e) {
            return PreferenceSource.MANUAL;
        }
    }

    private static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
