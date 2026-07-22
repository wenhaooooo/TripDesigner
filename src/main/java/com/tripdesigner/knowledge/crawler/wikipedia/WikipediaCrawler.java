package com.tripdesigner.knowledge.crawler.wikipedia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.crawler.DataCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wikipedia API 爬虫。
 *
 * <p>通过 MediaWiki API 和 REST API 抓取 Wikipedia 页面摘要和完整内容。
 *
 * <p>API 端点：
 * <ul>
 *   <li>MediaWiki API：{@code https://en.wikipedia.org/w/api.php} — 获取完整页面 wikitext 和修订 ID</li>
 *   <li>REST API：{@code https://en.wikipedia.org/api/rest_v1/page/summary/{title}} — 获取页面摘要</li>
 * </ul>
 *
 * <p>特性：
 * <ul>
 *   <li>支持增量抓取（基于页面修订 ID / revision ID）</li>
 *   <li>同时获取摘要（REST API）和完整内容（MediaWiki API）</li>
 *   <li>请求限速：每次请求间隔至少 500 毫秒</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikipediaCrawler implements DataCrawler {

    private static final String MEDIAWIKI_API_URL = "https://en.wikipedia.org/w/api.php";
    private static final String REST_API_SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";

    /** 请求限速间隔（毫秒） */
    private static final long RATE_LIMIT_MS = 500L;

    private final ObjectMapper objectMapper;

    /** Wikipedia API 专用 RestClient */
    private final RestClient restClient = buildRestClient();

    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public List<Map<String, Object>> crawl(String query, int limit) {
        log.info("[WikipediaCrawler] Crawling Wikipedia for: {}, limit: {}", query, limit);
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            // 1. 获取页面摘要（REST API）
            Map<String, Object> summaryData = fetchSummary(query);
            if (summaryData != null) {
                results.add(summaryData);
            }

            // 2. 获取完整页面内容（MediaWiki API）
            Map<String, Object> fullPageData = fetchFullPage(query);
            if (fullPageData != null) {
                results.add(fullPageData);
            }

            log.info("[WikipediaCrawler] Crawled {} items from Wikipedia for: {}", results.size(), query);
            rateLimit();
        } catch (Exception e) {
            log.warn("[WikipediaCrawler] Failed to crawl Wikipedia for {}: {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getSourceName() {
        return "WIKIPEDIA";
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public String crawlWithHash(String query, int limit, String lastHash) {
        log.debug("[WikipediaCrawler] Incremental crawl for: {}, lastHash: {}", query, lastHash);

        try {
            // 轻量级请求：仅获取页面修订 ID
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("en.wikipedia.org")
                            .path("/w/api.php")
                            .queryParam("action", "query")
                            .queryParam("titles", query)
                            .queryParam("prop", "revisions")
                            .queryParam("rvprop", "ids")
                            .queryParam("format", "json")
                            .queryParam("redirects", "true")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("query")) {
                return lastHash;
            }

            JsonNode pages = response.get("query").path("pages");
            if (pages.isObject()) {
                for (JsonNode page : pages) {
                    long revid = page.path("revisions").path(0).path("revid").asLong();
                    String currentHash = String.valueOf(revid);
                    if (currentHash.equals(lastHash)) {
                        log.debug("[WikipediaCrawler] No changes for: {} (revid={})", query, revid);
                        return lastHash;
                    }
                    return currentHash;
                }
            }

            return lastHash;
        } catch (Exception e) {
            log.warn("[WikipediaCrawler] Incremental check failed for {}: {}", query, e.getMessage());
            return lastHash;
        }
    }

    /**
     * 通过 REST API 获取页面摘要。
     */
    private Map<String, Object> fetchSummary(String title) {
        try {
            JsonNode summary = restClient.get()
                    .uri(REST_API_SUMMARY_URL + title)
                    .retrieve()
                    .body(JsonNode.class);

            if (summary == null) {
                return null;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("sourceType", "WIKIPEDIA_SUMMARY");
            data.put("title", summary.path("title").asText());
            data.put("summary", summary.path("extract").asText());
            data.put("description", summary.path("description").asText());

            // 坐标信息
            JsonNode coords = summary.path("coordinates");
            if (!coords.isMissingNode()) {
                data.put("latitude", coords.path("lat").asDouble());
                data.put("longitude", coords.path("lon").asDouble());
            }

            return data;
        } catch (Exception e) {
            log.debug("[WikipediaCrawler] Failed to fetch summary for {}: {}", title, e.getMessage());
            return null;
        }
    }

    /**
     * 通过 MediaWiki API 获取完整页面内容。
     */
    private Map<String, Object> fetchFullPage(String title) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("en.wikipedia.org")
                            .path("/w/api.php")
                            .queryParam("action", "parse")
                            .queryParam("page", title)
                            .queryParam("prop", "wikitext|revid|categories")
                            .queryParam("format", "json")
                            .queryParam("redirects", "true")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("parse")) {
                return null;
            }

            JsonNode parseNode = response.get("parse");
            Map<String, Object> data = new HashMap<>();
            data.put("sourceType", "WIKIPEDIA_FULL");
            data.put("title", parseNode.path("title").asText());
            data.put("pageId", parseNode.path("pageid").asLong());
            data.put("revisionId", parseNode.path("revid").asLong());
            data.put("wikitext", parseNode.path("wikitext").path("*").asText());

            // 提取分类
            List<String> categories = new ArrayList<>();
            JsonNode catNode = parseNode.path("categories");
            if (catNode.isArray()) {
                for (JsonNode cat : catNode) {
                    categories.add(cat.path("*").asText());
                }
            }
            data.put("categories", categories);

            return data;
        } catch (Exception e) {
            log.debug("[WikipediaCrawler] Failed to fetch full page for {}: {}", title, e.getMessage());
            return null;
        }
    }

    private void rateLimit() {
        try {
            Thread.sleep(RATE_LIMIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
