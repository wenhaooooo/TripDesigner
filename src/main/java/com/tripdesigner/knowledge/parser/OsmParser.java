package com.tripdesigner.knowledge.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OSM Overpass JSON 解析器。
 *
 * <p>将 OSM Overpass API 返回的 JSON 数据解析为结构化的 POI 数据。
 * 主要提取 OSM 标签（tags）中的旅游相关信息并转换为统一格式。
 *
 * <p>提取的标签字段：
 * <ul>
 *   <li>name — POI 名称</li>
 *   <li>tourism — 旅游类型（hotel、attraction、museum 等）</li>
 *   <li>amenity — 设施类型（restaurant、cafe、bar 等）</li>
 *   <li>historic — 历史遗迹类型</li>
 *   <li>description — 描述信息</li>
 *   <li>website — 官方网站</li>
 *   <li>opening_hours — 营业时间</li>
 *   <li>coordinates — 经纬度坐标</li>
 * </ul>
 *
 * <p>解析后的数据格式：
 * <pre>
 * {
 *   "name": "Tokyo Tower",
 *   "poiType": "attraction",
 *   "category": "tourism",
 *   "latitude": 35.6586,
 *   "longitude": 139.7454,
 *   "tags": { ... },
 *   "description": "..."
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OsmParser implements DataParser {

    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> parse(String rawContent, String sourceType) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (rawContent == null || rawContent.isBlank()) {
            log.debug("[OsmParser] Empty content, returning empty result");
            return result;
        }

        try {
            // rawContent 可能是单个 POI 的 JSON 字符串，也可能是包含 elements 数组的完整响应
            JsonNode root = objectMapper.readTree(rawContent);

            // 如果是完整的 Overpass 响应（含 elements 数组），取第一个元素
            if (root.has("elements") && root.get("elements").isArray()) {
                JsonNode elements = root.get("elements");
                if (!elements.isEmpty()) {
                    root = elements.get(0);
                }
            }

            // 如果有 tags 子节点，从 tags 中提取信息
            JsonNode tags = root.path("tags");
            if (tags.isObject()) {
                extractFromTags(tags, result);
            }

            // 提取坐标
            if (root.has("lat") && root.has("lon")) {
                result.put("latitude", root.get("lat").asDouble());
                result.put("longitude", root.get("lon").asDouble());
            }

            // 提取 OSM ID 和类型
            if (root.has("id")) {
                result.put("osmId", root.get("id").asLong());
            }
            if (root.has("type")) {
                result.put("osmType", root.get("type").asText());
            }

            log.debug("[OsmParser] Parsed OSM POI: {}", result.get("name"));
        } catch (Exception e) {
            log.warn("[OsmParser] Failed to parse OSM content: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public boolean supports(String sourceType) {
        return "OSM".equalsIgnoreCase(sourceType)
                || "OPENTRIPMAP".equalsIgnoreCase(sourceType);
    }

    /**
     * 从 OSM tags 中提取结构化信息。
     *
     * @param tags   OSM tags JSON 节点
     * @param result 输出的结果 Map
     */
    private void extractFromTags(JsonNode tags, Map<String, Object> result) {
        // 保留所有标签作为 tags 字段
        Map<String, String> tagMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = tags.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            tagMap.put(field.getKey(), field.getValue().asText());
        }
        result.put("tags", tagMap);

        // 提取名称
        if (tags.has("name")) {
            result.put("name", tags.get("name").asText());
        }
        if (tags.has("name:en")) {
            result.put("nameEn", tags.get("name:en").asText());
        }

        // 确定 POI 类型和分类
        if (tags.has("tourism")) {
            result.put("category", "tourism");
            result.put("poiType", tags.get("tourism").asText());
        } else if (tags.has("amenity")) {
            result.put("category", "amenity");
            result.put("poiType", tags.get("amenity").asText());
        } else if (tags.has("historic")) {
            result.put("category", "historic");
            result.put("poiType", tags.get("historic").asText());
        }

        // 提取描述和附加信息
        if (tags.has("description")) {
            result.put("description", tags.get("description").asText());
        }
        if (tags.has("website")) {
            result.put("website", tags.get("website").asText());
        }
        if (tags.has("cuisine")) {
            result.put("cuisine", tags.get("cuisine").asText());
        }
        if (tags.has("opening_hours")) {
            result.put("openingHours", tags.get("opening_hours").asText());
        }
        if (tags.has("phone")) {
            result.put("phone", tags.get("phone").asText());
        }
        if (tags.has("addr:full")) {
            result.put("address", tags.get("addr:full").asText());
        }
    }
}
