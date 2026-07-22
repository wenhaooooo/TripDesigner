package com.tripdesigner.weather.api.vo;
/**
 * 天气信息视图对象（VO）。
 * 用于封装目的地的天气预报数据，返回给前端展示。
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo {

    /** 目的地名称 */
    private String destination;

    /** 每日天气预报列表 */
    private List<DailyForecast> dailyForecasts;

    /**
     * 单日天气预报。
     * 包含日期、温度、降水量、天气编码、天气描述和风速等信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        /** 日期 */
        private LocalDate date;

        /** 最高温度（摄氏度） */
        private double maxTemp;

        /** 最低温度（摄氏度） */
        private double minTemp;

        /** 降水量（毫米） */
        private double precipitation;

        /** WMO 天气编码 */
        private int weatherCode;

        /** 天气描述（中文） */
        private String weatherDescription;

        /** 最大风速（km/h） */
        private double windSpeed;
    }
}
