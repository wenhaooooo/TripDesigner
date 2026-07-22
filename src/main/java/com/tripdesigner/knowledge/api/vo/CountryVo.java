package com.tripdesigner.knowledge.api.vo;

import java.time.Instant;
import java.util.List;

/**
 * 国家知识视图对象。
 *
 * 用于 API 响应中返回国家的基本信息，包括 ISO 编码、首都、货币、语言等。
 * 数据来源可能是 OSM、Wikivoyage、Wikipedia 等，通过 source 字段标识。
 *
 * @param id            国家 ID
 * @param name          国家名称（英文）
 * @param isoCode2      ISO 3166-1 alpha-2 国家代码（如 "US", "CN"）
 * @param isoCode3      ISO 3166-1 alpha-3 国家代码（如 "USA", "CHN"）
 * @param continent     所在大洲（如 "Asia", "Europe"）
 * @param capital       首都名称
 * @param currencyCode  货币代码（ISO 4217，如 "USD", "CNY"）
 * @param languages     官方语言列表
 * @param source        数据来源（OSM, WIKIVOYAGE, WIKIPEDIA 等）
 * @param lastSyncedAt  最近一次同步时间
 */
public record CountryVo(
        Long id,
        String name,
        String isoCode2,
        String isoCode3,
        String continent,
        String capital,
        String currencyCode,
        List<String> languages,
        String source,
        Instant lastSyncedAt
) {
}
