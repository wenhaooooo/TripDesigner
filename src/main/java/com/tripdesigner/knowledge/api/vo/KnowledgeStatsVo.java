package com.tripdesigner.knowledge.api.vo;

import java.util.Map;

/**
 * 知识库统计信息视图对象。
 *
 * 用于 API 响应中返回知识库的整体统计信息，包括各实体类型的总数和按数据源分布的数量。
 * 可用于监控知识库健康度、数据覆盖度以及数据源同步状态。
 *
 * @param totalCountries   国家总数
 * @param totalCities      城市总数
 * @param totalPois        POI 总数
 * @param totalRestaurants 餐厅总数
 * @param totalHotels      酒店总数
 * @param totalGuides      旅行攻略总数
 * @param totalChunks      知识块（向量化片段）总数
 * @param totalRoutes      路线总数
 * @param sourceCounts     按数据源统计的数量（key: source, value: count）
 */
public record KnowledgeStatsVo(
        long totalCountries,
        long totalCities,
        long totalPois,
        long totalRestaurants,
        long totalHotels,
        long totalGuides,
        long totalChunks,
        long totalRoutes,
        Map<String, Long> sourceCounts
) {
}
