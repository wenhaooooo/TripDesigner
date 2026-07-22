package com.tripdesigner.knowledge.crawler;

import java.util.List;
import java.util.Map;

/**
 * 数据爬虫接口。
 *
 * 定义了全球旅行知识库的数据采集契约。每个实现类负责从特定数据源
 * （如 OpenStreetMap、Wikivoyage、Wikipedia、OpenTripMap 等）抓取原始数据。
 *
 * <p>采集流程：
 * <ol>
 *   <li>{@link #crawl(String, int)} — 全量抓取，返回原始数据列表</li>
 *   <li>{@link #crawlWithHash(String, int, String)} — 增量抓取，基于上次的哈希值判断是否有新数据</li>
 * </ol>
 *
 * <p>每个数据项以 {@code Map<String, Object>} 表示，包含名称、坐标、描述等字段，
 * 具体结构由各爬虫实现决定，后续由 {@link com.tripdesigner.knowledge.parser.DataParser} 解析。
 */
public interface DataCrawler {

    /**
     * 全量抓取数据。
     *
     * @param query 查询条件（如目的地名称、坐标范围等）
     * @param limit 最大返回条数
     * @return 原始数据列表，每条数据为一个键值对 Map
     */
    List<Map<String, Object>> crawl(String query, int limit);

    /**
     * 获取数据源名称（如 OSM、WIKIVOYAGE、WIKIPEDIA、OPENTRIPMAP）。
     *
     * @return 数据源标识符
     */
    String getSourceName();

    /**
     * 是否支持增量抓取。
     * 增量抓取通过比较内容哈希值跳过未变更的数据，减少 API 调用。
     *
     * @return true 表示支持增量抓取
     */
    boolean supportsIncremental();

    /**
     * 增量抓取：基于上次的内容哈希值判断是否有新数据。
     *
     * <p>实现逻辑：
     * <ul>
     *   <li>如果当前内容哈希与 {@code lastHash} 相同，说明数据未变更，返回原哈希</li>
     *   <li>如果不同，说明有新数据，返回新哈希（调用方需再调用 {@link #crawl} 获取完整数据）</li>
     * </ul>
     *
     * @param query    查询条件
     * @param limit    最大返回条数
     * @param lastHash 上次抓取的内容哈希值，首次抓取时传 null
     * @return 当前内容哈希值；如果与 lastHash 相同则表示无新数据
     */
    String crawlWithHash(String query, int limit, String lastHash);
}
