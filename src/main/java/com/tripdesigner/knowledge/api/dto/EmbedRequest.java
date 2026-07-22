package com.tripdesigner.knowledge.api.dto;

/**
 * 手动向量化请求 DTO。
 *
 * 允许外部系统手动提交一段文本内容进行向量化并存储到知识库。
 * 适用于临时性的知识注入场景，如管理员手动添加旅行攻略、人工校对后的内容等。
 *
 * @param entityType 实体类型：COUNTRY, CITY, POI, RESTAURANT, HOTEL, TRAVEL_GUIDE, ROUTE
 * @param entityId   关联的实体 ID
 * @param content    待向量化的文本内容
 * @param chunkType  知识块类型（如 "description", "guide", "review" 等）
 * @param language   内容语言代码（如 "en", "zh" 等）
 */
public record EmbedRequest(
        String entityType,
        Long entityId,
        String content,
        String chunkType,
        String language
) {
}
