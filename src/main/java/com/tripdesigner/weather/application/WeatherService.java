package com.tripdesigner.weather.application;
/**
 * 天气服务。
 *
 * 调用 Open-Meteo 免费 API（无需 API Key）获取目的地天气数据：
 * - Geocoding API：将地名解析为经纬度坐标
 * - Forecast API：根据坐标获取每日天气预报
 *
 * 特性：
 * - 使用 RestClient（Spring 6.1+）调用 API
 * - 天气数据缓存到 Redis（key: weather:{destination}，TTL 30 分钟）
 * - API 调用失败时返回默认天气信息，不抛异常
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.weather.api.vo.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_API = "https://api.open-meteo.com/v1/forecast";

    /** Redis 缓存 key 前缀 */
    private static final String CACHE_KEY_PREFIX = "weather:";

    /** 缓存 TTL：30 分钟 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 天气 API 专用 RestClient（短超时，避免阻塞调用方） */
    private final RestClient weatherRestClient = buildWeatherRestClient();

    /**
     * 构建天气 API 专用的 RestClient。
     * 使用 SimpleClientHttpRequestFactory 并设置 10 秒超时，
     * 不复用 SpringAiConfig 中配置的长超时 RestClient（后者用于 LLM 调用）。
     */
    private RestClient buildWeatherRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 获取目的地的天气预报。
     * 先从 Redis 缓存读取，未命中则调用 Open-Meteo API。
     * 缓存 key 为 weather:{destination}，TTL 30 分钟。
     *
     * @param destination 目的地名称（如 "东京"、"巴黎"）
     * @param startDate   起始日期
     * @param endDate     结束日期
     * @return 天气信息；API 调用失败时返回默认天气信息
     */
    public WeatherInfo getWeather(String destination, LocalDate startDate, LocalDate endDate) {
        if (destination == null || destination.isBlank()) {
            log.warn("[WeatherService] destination is blank, returning default weather info");
            return defaultWeatherInfo(destination);
        }

        String cacheKey = CACHE_KEY_PREFIX + destination;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                WeatherInfo cachedInfo = objectMapper.readValue(cached, WeatherInfo.class);
                // 过滤出指定日期范围内的预报
                return filterByDateRange(cachedInfo, startDate, endDate);
            }
        } catch (Exception e) {
            log.warn("[WeatherService] Failed to read weather cache for {}: {}", destination, e.getMessage());
        }

        try {
            double[] coords = getDestinationCoords(destination);
            if (coords == null) {
                log.warn("[WeatherService] Coordinates not found for destination: {}", destination);
                return defaultWeatherInfo(destination);
            }
            WeatherInfo info = fetchWeatherFromApi(destination, coords[0], coords[1], startDate, endDate);

            // 缓存到 Redis（失败不影响主流程）
            try {
                String json = objectMapper.writeValueAsString(info);
                stringRedisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            } catch (Exception e) {
                log.warn("[WeatherService] Failed to cache weather for {}: {}", destination, e.getMessage());
            }

            return info;
        } catch (Exception e) {
            log.warn("[WeatherService] Failed to get weather for {}: {}", destination, e.getMessage());
            return defaultWeatherInfo(destination);
        }
    }

    /**
     * 按坐标获取天气预报。
     *
     * @param lat       纬度
     * @param lon       经度
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 天气信息；API 调用失败时返回默认天气信息
     */
    public WeatherInfo getWeatherByCoords(double lat, double lon, LocalDate startDate, LocalDate endDate) {
        try {
            return fetchWeatherFromApi(String.format("(%.4f,%.4f)", lat, lon), lat, lon, startDate, endDate);
        } catch (Exception e) {
            log.warn("[WeatherService] Failed to get weather by coords ({},{}): {}", lat, lon, e.getMessage());
            return defaultWeatherInfo(String.format("(%.4f,%.4f)", lat, lon));
        }
    }

    /**
     * 使用 Geocoding API 获取目的地的经纬度坐标。
     *
     * @param destination 目的地名称
     * @return double[]{latitude, longitude}，未找到或失败时返回 null
     */
    public double[] getDestinationCoords(String destination) {
        try {
            JsonNode root = weatherRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("geocoding-api.open-meteo.com")
                            .path("/v1/search")
                            .queryParam("name", destination)
                            .queryParam("count", 1)
                            .queryParam("language", "zh")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !root.has("results") || root.get("results").isNull() || !root.get("results").isArray() || root.get("results").isEmpty()) {
                log.warn("[WeatherService] No geocoding results for destination: {}", destination);
                return null;
            }

            JsonNode first = root.get("results").get(0);
            double lat = first.get("latitude").asDouble();
            double lon = first.get("longitude").asDouble();
            return new double[]{lat, lon};
        } catch (Exception e) {
            log.warn("[WeatherService] Geocoding API call failed for {}: {}", destination, e.getMessage());
            return null;
        }
    }

    /**
     * 调用 Open-Meteo Forecast API 获取天气预报并解析为 WeatherInfo。
     */
    private WeatherInfo fetchWeatherFromApi(String destination, double lat, double lon,
                                            LocalDate startDate, LocalDate endDate) {
        JsonNode root = weatherRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.open-meteo.com")
                        .path("/v1/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode,windspeed_10m_max")
                        .queryParam("timezone", "auto")
                        .queryParam("start_date", startDate.toString())
                        .queryParam("end_date", endDate.toString())
                        .build())
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !root.has("daily")) {
            log.warn("[WeatherService] No daily forecast in API response for {}", destination);
            return defaultWeatherInfo(destination);
        }

        JsonNode daily = root.get("daily");
        JsonNode times = daily.get("time");
        JsonNode maxTemps = daily.get("temperature_2m_max");
        JsonNode minTemps = daily.get("temperature_2m_min");
        JsonNode precip = daily.get("precipitation_sum");
        JsonNode codes = daily.get("weathercode");
        JsonNode winds = daily.get("windspeed_10m_max");

        if (times == null || !times.isArray() || times.isEmpty()) {
            log.warn("[WeatherService] Empty time array in forecast for {}", destination);
            return defaultWeatherInfo(destination);
        }

        List<WeatherInfo.DailyForecast> forecasts = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            forecasts.add(WeatherInfo.DailyForecast.builder()
                    .date(LocalDate.parse(times.get(i).asText()))
                    .maxTemp(safeDouble(maxTemps, i))
                    .minTemp(safeDouble(minTemps, i))
                    .precipitation(safeDouble(precip, i))
                    .weatherCode(safeInt(codes, i))
                    .weatherDescription(describeWeatherCode(safeInt(codes, i)))
                    .windSpeed(safeDouble(winds, i))
                    .build());
        }

        return WeatherInfo.builder()
                .destination(destination)
                .dailyForecasts(forecasts)
                .build();
    }

    /**
     * 按日期范围过滤缓存中的天气预报。
     */
    private WeatherInfo filterByDateRange(WeatherInfo cached, LocalDate startDate, LocalDate endDate) {
        if (cached == null || cached.getDailyForecasts() == null) {
            return cached;
        }
        List<WeatherInfo.DailyForecast> filtered = cached.getDailyForecasts().stream()
                .filter(f -> f.getDate() != null)
                .filter(f -> (startDate == null || !f.getDate().isBefore(startDate)))
                .filter(f -> (endDate == null || !f.getDate().isAfter(endDate)))
                .toList();
        return WeatherInfo.builder()
                .destination(cached.getDestination())
                .dailyForecasts(filtered)
                .build();
    }

    /**
     * 构造默认天气信息（API 调用失败时使用）。
     */
    private WeatherInfo defaultWeatherInfo(String destination) {
        return WeatherInfo.builder()
                .destination(destination)
                .dailyForecasts(List.of())
                .build();
    }

    private double safeDouble(JsonNode node, int i) {
        if (node == null || node.isNull() || i >= node.size() || node.get(i).isNull()) {
            return 0.0;
        }
        return node.get(i).asDouble();
    }

    private int safeInt(JsonNode node, int i) {
        if (node == null || node.isNull() || i >= node.size() || node.get(i).isNull()) {
            return 0;
        }
        return node.get(i).asInt();
    }

    /**
     * 将 WMO 天气编码转换为中文描述。
     * 完整编码表参见 Open-Meteo 文档。
     */
    private String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1 -> "大部晴";
            case 2 -> "多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻毛毛雨";
            case 61 -> "小雨";
            case 63 -> "中雨";
            case 65 -> "大雨";
            case 66, 67 -> "冻雨";
            case 71 -> "小雪";
            case 73 -> "中雪";
            case 75 -> "大雪";
            case 77 -> "米雪";
            case 80 -> "阵雨";
            case 81 -> "强阵雨";
            case 82 -> "暴雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷阵雨";
            case 96, 99 -> "雷阵雨伴冰雹";
            default -> "未知";
        };
    }
}
