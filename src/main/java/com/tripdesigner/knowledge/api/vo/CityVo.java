package com.tripdesigner.knowledge.api.vo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 城市知识视图对象。
 *
 * 用于 API 响应中返回城市的基本信息，包括所属国家、时区、人口、经纬度等。
 * 城市是连接国家与 POI/餐厅/酒店的中间层实体。
 *
 * @param id            城市 ID
 * @param countryId     所属国家 ID
 * @param countryName   所属国家名称（冗余字段，便于前端展示）
 * @param name          城市名称（英文）
 * @param nameLocal     城市本地名称（如 "Paris" 的本地名 "Paris"）
 * @param timezone      时区标识（如 "Europe/Paris"）
 * @param population    城市人口
 * @param latitude      纬度
 * @param longitude     经度
 * @param source        数据来源（OSM, WIKIVOYAGE, WIKIPEDIA 等）
 * @param lastSyncedAt  最近一次同步时间
 */
public record CityVo(
        Long id,
        Long countryId,
        String countryName,
        String name,
        String nameLocal,
        String timezone,
        Integer population,
        BigDecimal latitude,
        BigDecimal longitude,
        String source,
        Instant lastSyncedAt
) {
}
