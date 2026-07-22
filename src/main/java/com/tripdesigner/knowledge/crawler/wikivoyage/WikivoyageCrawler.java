package com.tripdesigner.knowledge.crawler.wikivoyage;

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
 * Wikivoyage API 爬虫。
 *
 * <p>通过 MediaWiki API 抓取 Wikivoyage 旅行指南页面的 wikitext 内容，
 * 包括页面正文和各章节（Introduction、Get in、See、Eat、Sleep 等）。
 *
 * <p>API 端点：{@code https://en.wikivoyage.org/w/api.php}
 *
 * <p>特性：
 * <ul>
 *   <li>支持增量抓取（基于页面修订 ID / revision ID）</li>
 *   <li>获取页面 wikitext 原文，由 {@link com.tripdesigner.knowledge.parser.WikivoyageParser} 解析</li>
 *   <li>请求限速：每次请求间隔至少 500 毫秒</li>
 * </ul>
 *
 * <p>MediaWiki API 调用参数：
 * <pre>
 * action=parse
 * page={title}
 * prop=wikitext|revid
 * format=json
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikivoyageCrawler implements DataCrawler {

    private static final String API_URL = "https://en.wikivoyage.org/w/api.php";

    /** 请求限速间隔（毫秒） */
    private static final long RATE_LIMIT_MS = 500L;

    private final ObjectMapper objectMapper;

    /** Wikivoyage API 专用 RestClient */
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
        log.info("[WikivoyageCrawler] Crawling Wikivoyage for: {}, limit: {}", query, limit);
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("en.wikivoyage.org")
                            .path("/w/api.php")
                            .queryParam("action", "parse")
                            .queryParam("page", query)
                            .queryParam("prop", "wikitext|revid|categories")
                            .queryParam("format", "json")
                            .queryParam("redirects", "true")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("parse")) {
                log.warn("[WikivoyageCrawler] No parse result for: {}", query);
                return results;
            }

            JsonNode parseNode = response.get("parse");
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("sourceType", "WIKIVOYAGE");
            pageData.put("title", parseNode.path("title").asText());
            pageData.put("pageId", parseNode.path("pageid").asLong());
            pageData.put("revisionId", parseNode.path("revid").asLong());
            pageData.put("wikitext", parseNode.path("wikitext").path("*").asText());

            // 提取分类
            List<String> categories = new ArrayList<>();
            JsonNode catNode = parseNode.path("categories");
            if (catNode.isArray()) {
                for (JsonNode cat : catNode) {
                    categories.add(cat.path("*").asText());
                }
            }
            pageData.put("categories", categories);

            results.add(pageData);
            log.info("[WikivoyageCrawler] Crawled Wikivoyage page: {}", pageData.get("title"));

            rateLimit();
        } catch (Exception e) {
            log.warn("[WikivoyageCrawler] Failed to crawl Wikivoyage for {}: {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getSourceName() {
        return "WIKIVOYAGE";
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public String crawlWithHash(String query, int limit, String lastHash) {
        log.debug("[WikivoyageCrawler] Incremental crawl for: {}, lastHash: {}", query, lastHash);

        try {
            // 轻量级请求：仅获取页面修订 ID，不获取完整内容
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("en.wikivoyage.org")
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
                        log.debug("[WikivoyageCrawler] No changes for: {} (revid={})", query, revid);
                        return lastHash;
                    }
                    return currentHash;
                }
            }

            return lastHash;
        } catch (Exception e) {
            log.warn("[WikivoyageCrawler] Incremental check failed for {}: {}", query, e.getMessage());
            return lastHash;
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
