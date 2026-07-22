package com.tripdesigner.knowledge.parser;

import java.util.Map;

/**
 * 数据解析器接口。
 *
 * 将爬虫抓取的原始内容（JSON、Wikitext、HTML 等）解析为结构化的 Map 数据，
 * 供后续的清洗、分块、向量化流程使用。
 *
 * <p>每个解析器负责一种数据源格式：
 * <ul>
 *   <li>{@link WikivoyageParser} — 解析 Wikivoyage wikitext</li>
 *   <li>{@link WikipediaParser} — 解析 Wikipedia 内容</li>
 *   <li>{@link OsmParser} — 解析 OSM Overpass JSON</li>
 * </ul>
 */
public interface DataParser {

    /**
     * 解析原始内容为结构化数据。
     *
     * @param rawContent 原始内容字符串（JSON、Wikitext 等）
     * @param sourceType 数据源类型标识（如 WIKIVOYAGE、WIKIPEDIA、OSM）
     * @return 解析后的结构化数据 Map，通常包含 title、sections、categories 等字段
     */
    Map<String, Object> parse(String rawContent, String sourceType);

    /**
     * 判断当前解析器是否支持指定的数据源类型。
     *
     * @param sourceType 数据源类型标识
     * @return true 表示当前解析器可以处理该数据源
     */
    boolean supports(String sourceType);
}
