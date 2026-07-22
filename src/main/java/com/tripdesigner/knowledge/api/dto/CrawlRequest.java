package com.tripdesigner.knowledge.api.dto;

/**
 * 知识库抓取请求 DTO。
 *
 * 用于触发知识库的数据抓取流程，支持多种数据源（OSM、Wikivoyage、Wikipedia、OpenTripMap）。
 * 可指定抓取关键词（如城市名 "Paris" 或国家名 "France"）、最大结果数以及是否启用增量更新。
 *
 * @param source       数据源标识：OSM, WIKIVOYAGE, WIKIPEDIA, OPENTRIPMAP
 * @param query        查询关键词，例如 "Paris" 或 "France"
 * @param limit        最大抓取结果数，默认 100
 * @param incremental  是否启用增量更新（仅抓取新增/变更的数据）
 */
public record CrawlRequest(
        String source,
        String query,
        Integer limit,
        boolean incremental
) {
    /**
     * 紧凑构造器：为可空字段提供默认值。
     */
    public CrawlRequest {
        if (limit == null) {
            limit = 100;
        }
    }
}
