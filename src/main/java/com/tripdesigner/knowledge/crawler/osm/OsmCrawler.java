package com.tripdesigner.knowledge.crawler.osm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.crawler.DataCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * OpenStreetMap Overpass API 爬虫。
 *
 * <p>通过 Overpass QL 查询 OpenStreetMap 中的旅游相关节点（POI），
 * 包括 tourism（景点）、amenity（设施）、historic（历史遗迹）等类别。
 *
 * <p>API 端点：{@code https://overpass-api.de/api/interpreter}
 *
 * <p>特性：
 * <ul>
 *   <li>支持增量抓取（基于区域哈希）</li>
 *   <li>请求限速：每次请求间隔至少 1 秒（Overpass 公共 API 限流要求）</li>
 *   <li>解析 Overpass JSON 响应，提取 POI 标签和坐标</li>
 * </ul>
 *
 * <p>Overpass QL 查询示例（查询指定区域内的旅游 POI）：
 * <pre>
 * [out:json];
 * area[name="Tokyo"]->.searchArea;
 * (
 *   node["tourism"](area.searchArea);
 *   node["amenity"~"restaurant|cafe"](area.searchArea);
 *   node["historic"](area.searchArea);
 * );
 * out {limit};
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OsmCrawler implements DataCrawler {

    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";

    /** 请求限速间隔（毫秒），Overpass 公共 API 要求至少 1 秒间隔 */
    private static final long RATE_LIMIT_MS = 1000L;

    private final ObjectMapper objectMapper;

    /** OSM API 专用 RestClient（30 秒超时，Overpass 查询可能较慢） */
    private final RestClient restClient = buildRestClient();

    /**
     * 构建 OSM API 专用的 RestClient。
     */
    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public List<Map<String, Object>> crawl(String query, int limit) {
        log.info("[OsmCrawler] Crawling OSM for query: {}, limit: {}", query, limit);
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            String overpassQuery = buildOverpassQuery(query, limit);
            JsonNode response = restClient.post()
                    .uri(OVERPASS_API_URL)
                    .body("data=" + overpassQuery)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("elements")) {
                log.warn("[OsmCrawler] No elements in Overpass response for query: {}", query);
                return results;
            }

            JsonNode elements = response.get("elements");
            for (JsonNode element : elements) {
                Map<String, Object> poi = parseElement(element);
                if (poi != null) {
                    results.add(poi);
                }
            }

            log.info("[OsmCrawler] Crawled {} POIs from OSM for query: {}", results.size(), query);
            rateLimit();
        } catch (Exception e) {
            log.warn("[OsmCrawler] Failed to crawl OSM for query {}: {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getSourceName() {
        return "OSM";
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public String crawlWithHash(String query, int limit, String lastHash) {
        log.debug("[OsmCrawler] Incremental crawl for query: {}, lastHash: {}", query, lastHash);
        List<Map<String, Object>> data = crawl(query, limit);
        String currentHash = computeHash(data);

        if (currentHash.equals(lastHash)) {
            log.debug("[OsmCrawler] No changes detected for query: {}", query);
            return lastHash;
        }

        return currentHash;
    }

    /**
     * 构建 Overpass QL 查询语句。
     * 查询指定区域内的 tourism、amenity、historic 节点。
     *
     * @param areaName 区域名称（如城市名）
     * @param limit    最大返回数量
     * @return Overpass QL 查询字符串
     */
    private String buildOverpassQuery(String areaName, int limit) {
        return String.format(
                "[out:json];" +
                "area[\"name\"=\"%s\"]->.searchArea;" +
                "(" +
                "  node[\"tourism\"](area.searchArea);" +
                "  node[\"amenity\"~\"restaurant|cafe|bar|fast_food|pub\"](area.searchArea);" +
                "  node[\"historic\"](area.searchArea);" +
                ");" +
                "out %d;",
                areaName, limit
        );
    }

    /**
     * 解析 Overpass JSON 中的单个元素为 POI Map。
     */
    private Map<String, Object> parseElement(JsonNode element) {
        try {
            if (!element.has("tags")) {
                return null;
            }

            JsonNode tags = element.get("tags");
            if (!tags.has("name")) {
                return null;
            }

            Map<String, Object> poi = new HashMap<>();
            poi.put("sourceType", "OSM");
            poi.put("osmId", element.path("id").asLong());
            poi.put("osmType", element.path("type").asText());

            if (element.has("lat") && element.has("lon")) {
                poi.put("latitude", element.get("lat").asDouble());
                poi.put("longitude", element.get("lon").asDouble());
            }

            poi.put("name", tags.get("name").asText());
            poi.put("tourism", tags.path("tourism").asText(null));
            poi.put("amenity", tags.path("amenity").asText(null));
            poi.put("historic", tags.path("historic").asText(null));
            poi.put("description", tags.path("description").asText(null));
            poi.put("website", tags.path("website").asText(null));
            poi.put("cuisine", tags.path("cuisine").asText(null));
            poi.put("openingHours", tags.path("opening_hours").asText(null));

            return poi;
        } catch (Exception e) {
            log.debug("[OsmCrawler] Failed to parse element: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算数据列表的内容哈希值（SHA-256）。
     */
    private String computeHash(List<Map<String, Object>> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("[OsmCrawler] Failed to compute hash: {}", e.getMessage());
            return String.valueOf(data.hashCode());
        }
    }

    /**
     * 请求限速：确保两次请求之间至少间隔 {@link #RATE_LIMIT_MS} 毫秒。
     */
    private void rateLimit() {
        try {
            Thread.sleep(RATE_LIMIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
