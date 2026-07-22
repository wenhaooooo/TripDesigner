package com.tripdesigner.knowledge.crawler.opentripmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.crawler.DataCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenTripMap API 爬虫。
 *
 * <p>通过 OpenTripMap API 抓取全球兴趣点（POI）数据，
 * 包括景点、博物馆、餐厅等旅游相关地点。
 *
 * <p>API 端点：{@code https://api.opentripmap.com/1.0/en/places/}
 *
 * <p>特性：
 * <ul>
 *   <li>需要 API Key（通过配置 {@code opentripmap.api.key} 注入）</li>
 *   <li>支持按 geonameId 或经纬度坐标查询</li>
 *   <li>不支持增量抓取（{@link #supportsIncremental()} 返回 false）</li>
 *   <li>请求限速：每次请求间隔至少 500 毫秒</li>
 * </ul>
 *
 * <p>API 调用示例：
 * <pre>
 * GET https://api.opentripmap.com/1.0/en/places/geoname?geonameid=1850147&apikey={key}
 * GET https://api.opentripmap.com/1.0/en/places/radius?radius=1000&lon=139.69&lat=35.68&apikey={key}
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenTripMapCrawler implements DataCrawler {

    private static final String API_BASE_URL = "https://api.opentripmap.com/1.0/en/places/";

    /** 请求限速间隔（毫秒） */
    private static final long RATE_LIMIT_MS = 500L;

    private final ObjectMapper objectMapper;

    /** OpenTripMap API Key，通过配置注入 */
    @Value("${opentripmap.api.key:}")
    private String apiKey;

    /** OpenTripMap API 专用 RestClient */
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
        log.info("[OpenTripMapCrawler] Crawling OpenTripMap for: {}, limit: {}", query, limit);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[OpenTripMapCrawler] API key not configured (opentripmap.api.key), skipping crawl");
            return List.of();
        }

        List<Map<String, Object>> results = new ArrayList<>();

        try {
            // 尝试按 geonameId 查询（query 可能为数字 ID）
            JsonNode response;
            if (query.matches("\\d+")) {
                response = fetchByGeonameId(query);
            } else {
                // 按名称搜索获取 geonameId，再按半径查询 POI
                response = fetchByRadius(query, limit);
            }

            if (response == null) {
                return results;
            }

            // 解析 POI 列表
            JsonNode features = response.path("features");
            if (features.isArray()) {
                int count = 0;
                for (JsonNode feature : features) {
                    if (count >= limit) {
                        break;
                    }
                    Map<String, Object> poi = parseFeature(feature);
                    if (poi != null) {
                        results.add(poi);
                        count++;
                    }
                }
            }

            log.info("[OpenTripMapCrawler] Crawled {} POIs from OpenTripMap for: {}", results.size(), query);
            rateLimit();
        } catch (Exception e) {
            log.warn("[OpenTripMapCrawler] Failed to crawl OpenTripMap for {}: {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getSourceName() {
        return "OPENTRIPMAP";
    }

    @Override
    public boolean supportsIncremental() {
        return false;
    }

    @Override
    public String crawlWithHash(String query, int limit, String lastHash) {
        // 不支持增量抓取，直接全量抓取并返回新哈希
        List<Map<String, Object>> data = crawl(query, limit);
        return String.valueOf(data.hashCode());
    }

    /**
     * 按 geonameId 查询地点信息。
     */
    private JsonNode fetchByGeonameId(String geonameId) {
        try {
            return restClient.get()
                    .uri(API_BASE_URL + "geoname?geonameid=" + geonameId + "&apikey=" + apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.debug("[OpenTripMapCrawler] fetchByGeonameId failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 按名称搜索后按半径查询周边 POI。
     */
    private JsonNode fetchByRadius(String name, int limit) {
        try {
            // 先通过 geoname 搜索获取坐标
            JsonNode geoname = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.opentripmap.com")
                            .path("/1.0/en/places/geoname")
                            .queryParam("name", name)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (geoname == null || !geoname.has("lat")) {
                log.debug("[OpenTripMapCrawler] No geoname result for: {}", name);
                return null;
            }

            double lat = geoname.path("lat").asDouble();
            double lon = geoname.path("lon").asDouble();

            // 按半径查询 POI
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.opentripmap.com")
                            .path("/1.0/en/places/radius")
                            .queryParam("radius", "10000")
                            .queryParam("lon", lon)
                            .queryParam("lat", lat)
                            .queryParam("rate", "3")
                            .queryParam("format", "json")
                            .queryParam("limit", limit)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.debug("[OpenTripMapCrawler] fetchByRadius failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 GeoJSON Feature 为 POI Map。
     */
    private Map<String, Object> parseFeature(JsonNode feature) {
        try {
            JsonNode properties = feature.path("properties");
            JsonNode geometry = feature.path("geometry");

            Map<String, Object> poi = new HashMap<>();
            poi.put("sourceType", "OPENTRIPMAP");
            poi.put("name", properties.path("name").asText());
            poi.put("kinds", properties.path("kinds").asText());
            poi.put("rate", properties.path("rate").asInt());
            poi.put("xid", properties.path("xid").asText());

            // 坐标（GeoJSON 格式：[lon, lat]）
            JsonNode coords = geometry.path("coordinates");
            if (coords.isArray() && coords.size() >= 2) {
                poi.put("longitude", coords.get(0).asDouble());
                poi.put("latitude", coords.get(1).asDouble());
            }

            return poi;
        } catch (Exception e) {
            log.debug("[OpenTripMapCrawler] Failed to parse feature: {}", e.getMessage());
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
