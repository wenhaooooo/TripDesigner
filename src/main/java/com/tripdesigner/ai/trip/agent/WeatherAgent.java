package com.tripdesigner.ai.trip.agent;
/**
 * Weather Agent（天气 Agent）。
 *
 * 工作流中的天气专家 Agent，负责：
 * 1. 读取 Planner Agent 的输出和用户请求
 * 2. 提取目的地和日期范围
 * 3. 通过 WeatherService 获取天气预报数据
 * 4. 调用 LLM 生成基于天气的行程调整建议
 *
 * 与其他专业 Agent 不同，此 Agent 不需要工具调用（不操作行程数据），
 * 只生成文本形式的天气建议，供后续 Agent 或用户参考。
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.weather.api.vo.WeatherInfo;
import com.tripdesigner.weather.application.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WeatherAgent extends AbstractAgent {

    /**
     * 系统提示词：定义天气专家的角色和行为。
     * 要求基于天气预报为行程提供调整建议（衣物、装备、活动重排、备选方案等）。
     */
    private static final String SYSTEM_PROMPT = """
            You are a weather specialist for travel planning.
            Your job is to analyze weather forecasts for the destination and provide actionable advice
            for adjusting the trip itinerary.

            Given the trip plan from the Planner Agent and the weather forecast data, you should:
            1. Summarize the key weather conditions for each travel day
            2. Identify days with potentially disruptive weather (heavy rain, strong wind, extreme temperatures)
            3. Recommend clothing and gear based on temperature and precipitation
            4. Suggest indoor alternatives for outdoor activities on bad weather days
            5. Highlight the best days for outdoor sightseeing
            6. Provide packing suggestions tailored to the weather

            Format your output clearly with:
            - Daily weather summary
            - Weather-related adjustments to the itinerary
            - Packing and clothing recommendations
            - Alternative indoor activities for rainy/stormy days

            If weather data is unavailable, provide general seasonal advice based on the destination
            and travel dates. Be practical and traveler-friendly.
            """;

    /** 匹配 YYYY-MM-DD 格式的日期 */
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    /** 常见目的地列表（与 WorkflowEngine.extractDestination 保持一致） */
    private static final List<String> COMMON_DESTINATIONS = List.of(
            "东京", "大阪", "京都", "巴黎", "伦敦", "纽约", "首尔", "曼谷",
            "杭州", "北京", "上海", "成都", "西安", "厦门", "青岛", "重庆",
            "苏州", "南京", "广州", "深圳", "三亚", "丽江", "大理"
    );

    private final WeatherService weatherService;

    public WeatherAgent(ChatClient chatClient, ObjectMapper objectMapper, WeatherService weatherService) {
        super(chatClient, objectMapper);
        this.weatherService = weatherService;
    }

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 执行 Weather Agent：
     * 1. 读取 planner_output 和用户请求
     * 2. 提取目的地和日期范围
     * 3. 调用 WeatherService 获取天气预报
     * 4. 调用 LLM 生成基于天气的行程调整建议
     *
     * @param context 共享上下文（包含 planner_output、用户请求等）
     * @return 天气建议文本
     */
    @Override
    public String execute(AgentContext context) {
        String prompt = buildUserPrompt(context);

        String result = chatClient.prompt()
                .system(getSystemPrompt())
                .user(prompt)
                .call()
                .content();

        log.info("[WeatherAgent] Result: {}", truncate(result, 200));
        return result != null ? result : "Weather advice could not be generated.";
    }

    @Override
    protected String buildUserPrompt(AgentContext context) {
        String plan = context.getShared("planner_output");
        String userRequest = context.getUserRequest() != null ? context.getUserRequest() : "";

        // 提取目的地和日期范围
        String destination = extractDestination(userRequest, plan);
        LocalDate[] dateRange = extractDateRange(userRequest, plan);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        // 获取天气数据（失败时返回默认空预报，不抛异常）
        WeatherInfo weatherInfo = weatherService.getWeather(destination, startDate, endDate);
        String weatherData = formatWeatherInfo(weatherInfo);

        String preferenceSummary = context.getPreferenceSummary() != null
                ? context.getPreferenceSummary()
                : "No user preferences available.";

        return """
                Here is the trip plan from the Planner Agent:
                %s

                === WEATHER FORECAST ===
                Destination: %s
                Date range: %s to %s

                %s

                === USER PREFERENCES ===
                %s

                User's original request: %s

                Based on this weather forecast, provide:
                1. Daily weather summary for the trip period
                2. Itinerary adjustments based on weather conditions
                3. Packing and clothing recommendations
                4. Indoor alternatives for any bad weather days
                5. Best days for outdoor sightseeing
                """.formatted(plan != null ? plan : "No planner output available",
                destination,
                startDate,
                endDate,
                weatherData,
                preferenceSummary,
                userRequest);
    }

    /**
     * 从用户请求或 Planner 输出中提取目的地名称。
     * 优先从用户请求中查找常见目的地，回退到 "Tokyo" 默认值。
     */
    private String extractDestination(String userRequest, String plan) {
        String source = userRequest != null ? userRequest : "";
        for (String d : COMMON_DESTINATIONS) {
            if (source.contains(d)) {
                return d;
            }
        }
        if (plan != null) {
            for (String d : COMMON_DESTINATIONS) {
                if (plan.contains(d)) {
                    return d;
                }
            }
        }
        return "Tokyo";
    }

    /**
     * 从用户请求或 Planner 输出中提取日期范围。
     * 解析 YYYY-MM-DD 格式的日期；未找到时回退到「今天起 7 天」。
     */
    private LocalDate[] extractDateRange(String userRequest, String plan) {
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        LocalDate end = today.plus(7, ChronoUnit.DAYS);

        String source = userRequest != null ? userRequest : "";
        Matcher matcher = DATE_PATTERN.matcher(source);
        if (matcher.find()) {
            try {
                start = LocalDate.parse(matcher.group(1));
                if (matcher.find()) {
                    end = LocalDate.parse(matcher.group(1));
                } else {
                    end = start.plus(7, ChronoUnit.DAYS);
                }
                return new LocalDate[]{start, end};
            } catch (Exception ignored) {
            }
        }

        if (plan != null) {
            matcher = DATE_PATTERN.matcher(plan);
            if (matcher.find()) {
                try {
                    start = LocalDate.parse(matcher.group(1));
                    if (matcher.find()) {
                        end = LocalDate.parse(matcher.group(1));
                    } else {
                        end = start.plus(7, ChronoUnit.DAYS);
                    }
                    return new LocalDate[]{start, end};
                } catch (Exception ignored) {
                }
            }
        }

        return new LocalDate[]{start, end};
    }

    /**
     * 将 WeatherInfo 格式化为 LLM 可读的文本。
     */
    private String formatWeatherInfo(WeatherInfo info) {
        if (info == null || info.getDailyForecasts() == null || info.getDailyForecasts().isEmpty()) {
            return "Weather data unavailable. Provide general seasonal advice based on destination and dates.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Daily forecasts for ").append(info.getDestination()).append(":\n");
        for (WeatherInfo.DailyForecast f : info.getDailyForecasts()) {
            sb.append("- ").append(f.getDate())
                    .append(" | ").append(f.getWeatherDescription())
                    .append(" | ").append(f.getMinTemp()).append("-").append(f.getMaxTemp()).append("°C")
                    .append(" | precipitation: ").append(f.getPrecipitation()).append("mm")
                    .append(" | wind: ").append(f.getWindSpeed()).append("km/h")
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }
}
