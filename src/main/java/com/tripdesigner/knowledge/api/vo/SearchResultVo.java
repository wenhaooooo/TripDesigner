package com.tripdesigner.knowledge.api.vo;

import java.util.Map;

/**
 * 知识库检索结果视图对象。
 *
 * 用于 API 响应中返回 RAG 语义检索的结果，包含知识块内容、相似度评分和元数据。
 * metadata 中可能包含 cityId、countryId、category 等过滤信息，便于前端进一步处理。
 *
 * @param chunkId    知识块 ID
 * @param entityType 实体类型：COUNTRY, CITY, POI, RESTAURANT, HOTEL, TRAVEL_GUIDE, ROUTE
 * @param entityId   关联的实体 ID
 * @param title      结果标题（通常为实体名称）
 * @param content    知识块文本内容
 * @param score      相似度评分（0-1，越高越相关）
 * @param chunkType  知识块类型（如 "description", "guide", "review"）
 * @param metadata   额外元数据（cityId, countryId, category, language 等）
 */
public record SearchResultVo(
        Long chunkId,
        String entityType,
        Long entityId,
        String title,
        String content,
        double score,
        String chunkType,
        Map<String, Object> metadata
) {
}
