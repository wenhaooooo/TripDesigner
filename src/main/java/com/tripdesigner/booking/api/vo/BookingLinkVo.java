package com.tripdesigner.booking.api.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 预订跳转链接 VO。
 * 包含多个预订平台的搜索 URL，前端可直接打开。
 */
@Getter
@Builder
public class BookingLinkVo {
    private final String platform;
    private final String platformName;
    private final String bookingUrl;
    private final String description;

    public static BookingLinkVo of(String platform, String platformName, String url, String description) {
        return BookingLinkVo.builder()
                .platform(platform)
                .platformName(platformName)
                .bookingUrl(url)
                .description(description)
                .build();
    }

    /** 预订建议：根据活动类别提供最佳平台 */
    public record Suggestion(String activityName, String category, List<BookingLinkVo> links) {}
}
