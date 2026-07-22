package com.tripdesigner.statistics.api.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 旅行统计仪表盘 VO。
 *
 * 汇总用户的行程、预算、目的地、活动等数据，用于可视化展示。
 */
@Getter
@Builder
public class TripStatisticsVo {
    private final Long userId;

    // 概览
    private final long totalTrips;
    private final long completedTrips;
    private final long planningTrips;
    private final long draftTrips;

    // 预算
    private final Integer totalBudget;
    private final Integer averageBudget;

    // 目的地
    private final List<Map<String, Object>> topDestinations;

    // 活动类别分布
    private final List<Map<String, Object>> activityCategoryDistribution;

    // 行程时长分布
    private final List<Map<String, Object>> tripDurationDistribution;

    // 月度统计
    private final List<Map<String, Object>> monthlyStats;

    // 成就徽章
    private final List<String> achievements;
}
