package com.tripdesigner.knowledge.api.vo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * POI（Point of Interest，兴趣点）视图对象。
 *
 * 用于 API 响应中返回景点/兴趣点的详细信息，包括分类、描述、经纬度、评分等。
 * POI 是知识库中最重要的实体类型，支撑行程规划、景点推荐等核心功能。
 *
 * @param id           POI ID
 * @param cityId       所属城市 ID
 * @param cityName     所属城市名称（冗余字段，便于前端展示）
 * @param name         POI 名称（英文）
 * @param nameLocal    POI 本地名称
 * @param category     主类别（如 "museum", "park", "church"）
 * @param subcategory  子类别（如 "art_museum", "national_park"）
 * @param description  POI 描述
 * @param latitude     纬度
 * @param longitude    经度
 * @param address      地址
 * @param rating       评分（0-5）
 * @param reviewCount  评论数
 * @param source       数据来源（OSM, OPENTRIPMAP, WIKIPEDIA 等）
 * @param lastSyncedAt 最近一次同步时间
 */
public record PoiVo(
        Long id,
        Long cityId,
        String cityName,
        String name,
        String nameLocal,
        String category,
        String subcategory,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        BigDecimal rating,
        Integer reviewCount,
        String source,
        Instant lastSyncedAt
) {
}
