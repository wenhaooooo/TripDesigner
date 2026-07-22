package com.tripdesigner.knowledge.api.dto;

/**
 * 知识库语义检索请求 DTO。
 *
 * 支持基于向量相似度的语义检索，可通过 entityType、cityId、countryId、category 等条件过滤，
 * 并支持 MMR（最大边际相关性）和混合检索（向量 + 关键词）两种高级检索模式。
 *
 * @param query               查询文本（必填）
 * @param entityType          实体类型过滤：COUNTRY, CITY, POI, RESTAURANT, HOTEL, TRAVEL_GUIDE, ROUTE（可空）
 * @param cityId              城市 ID 过滤（可空）
 * @param countryId           国家 ID 过滤（可空）
 * @param category            类别过滤（可空，如 "museum", "restaurant" 等）
 * @param language            语言代码，默认 "en"
 * @param topK                返回结果数，默认 5
 * @param similarityThreshold 相似度阈值，默认 0.5
 * @param useMmr              是否启用 MMR（最大边际相关性）去重，默认 false
 * @param useHybridSearch     是否启用混合检索（向量 + 关键词），默认 false
 */
public record SearchRequest(
        String query,
        String entityType,
        Long cityId,
        Long countryId,
        String category,
        String language,
        Integer topK,
        Double similarityThreshold,
        boolean useMmr,
        boolean useHybridSearch
) {
    /**
     * 紧凑构造器：为可空字段及带默认值的字段提供合理默认值。
     */
    public SearchRequest {
        if (language == null || language.isBlank()) {
            language = "en";
        }
        if (topK == null) {
            topK = 5;
        }
        if (similarityThreshold == null) {
            similarityThreshold = 0.5;
        }
    }
}
