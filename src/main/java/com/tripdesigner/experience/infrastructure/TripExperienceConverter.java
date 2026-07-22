package com.tripdesigner.experience.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.experience.domain.ExperienceStatus;
import com.tripdesigner.experience.domain.TripExperience;

import java.util.List;

/**
 * TripExperience 转换器。
 * 负责领域实体（TripExperience）和持久化对象（TripExperiencePO）之间的双向转换。
 * 处理 tags 和 mediaUrls 的 JSON 序列化/反序列化。
 */
public class TripExperienceConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * PO → 领域实体转换。
     * tags 和 mediaUrls 从 JSON 字符串解析为 List<String>。
     */
    public static TripExperience toDomain(TripExperiencePO po) {
        if (po == null) return null;
        return TripExperience.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .tripId(po.getTripId())
                .tripDayId(po.getTripDayId())
                .tripActivityId(po.getTripActivityId())
                .title(po.getTitle())
                .content(po.getContent())
                .rating(po.getRating())
                .tags(parseList(po.getTags()))
                .mediaUrls(parseList(po.getMediaUrls()))
                .status(mapStatus(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }

    /**
     * 领域实体 → PO 转换。
     * tags 和 mediaUrls 从 List<String> 序列化为 JSON 字符串。
     */
    public static TripExperiencePO toPO(TripExperience domain) {
        if (domain == null) return null;
        TripExperiencePO po = new TripExperiencePO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setTripId(domain.getTripId());
        po.setTripDayId(domain.getTripDayId());
        po.setTripActivityId(domain.getTripActivityId());
        po.setTitle(domain.getTitle());
        po.setContent(domain.getContent());
        po.setRating(domain.getRating());
        po.setTags(serializeList(domain.getTags()));
        po.setMediaUrls(serializeList(domain.getMediaUrls()));
        po.setStatus(mapStatus(domain.getStatus()));
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setVersion(domain.getVersion());
        return po;
    }

    /** 枚举 → 数据库字符串 */
    private static String mapStatus(ExperienceStatus status) {
        return status != null ? status.name() : "PUBLISHED";
    }

    /** 数据库字符串 → 枚举 */
    private static ExperienceStatus mapStatus(String status) {
        try {
            return ExperienceStatus.valueOf(status);
        } catch (Exception e) {
            return ExperienceStatus.PUBLISHED;
        }
    }

    /** JSON 字符串 → List<String> */
    private static List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of(json);
        }
    }

    /** List<String> → JSON 字符串 */
    private static String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return list.toString();
        }
    }
}
