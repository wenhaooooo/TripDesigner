package com.tripdesigner.checkin.api.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 打卡统计 VO。
 */
@Getter
@Builder
public class CheckinStatsVo {
    private final Long userId;
    private final long totalCheckins;
    private final long completedCount;
    private final long skippedCount;
    private final List<Map<String, Object>> recentCheckins;
    private final List<String> visitedPlaces;
}
