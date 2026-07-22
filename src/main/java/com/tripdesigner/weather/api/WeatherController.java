package com.tripdesigner.weather.api;
/**
 * 天气 REST API 控制器。
 *
 * 提供目的地天气预报查询接口，支持按地名或经纬度查询：
 * - GET /weather?destination=东京&startDate=...&endDate=... 按地名查询
 * - GET /weather/coords?lat=35.68&lon=139.76&startDate=...&endDate=... 按坐标查询
 *
 * 所有响应使用 Result<T> 统一格式。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.weather.api.vo.WeatherInfo;
import com.tripdesigner.weather.application.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 按目的地名称获取天气预报。
     *
     * @param destination 目的地名称（如 "东京"、"巴黎"）
     * @param startDate   起始日期（ISO 格式，如 "2026-08-01"）
     * @param endDate     结束日期（ISO 格式，如 "2026-08-05"）
     * @return 天气信息
     */
    @GetMapping
    public Result<WeatherInfo> getWeather(@RequestParam String destination,
                                          @RequestParam String startDate,
                                          @RequestParam String endDate) {
        return Result.success(weatherService.getWeather(
                destination,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate)));
    }

    /**
     * 按经纬度坐标获取天气预报。
     *
     * @param lat       纬度
     * @param lon       经度
     * @param startDate 起始日期（ISO 格式）
     * @param endDate   结束日期（ISO 格式）
     * @return 天气信息
     */
    @GetMapping("/coords")
    public Result<WeatherInfo> getWeatherByCoords(@RequestParam double lat,
                                                  @RequestParam double lon,
                                                  @RequestParam String startDate,
                                                  @RequestParam String endDate) {
        return Result.success(weatherService.getWeatherByCoords(
                lat,
                lon,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate)));
    }
}
