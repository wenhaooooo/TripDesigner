package com.tripdesigner.behavior.api.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 偏好分析结果 VO。
 *
 * 根据用户近期行为统计的偏好画像，包含：
 * - 偏好的目的地（按交互次数+权重排序）
 * - 偏好的活动类别
 * - 高频搜索关键词
 * - 推荐权重（用于 AI 推荐 Prompt 注入）
 */
@Getter
@Builder
public class PreferenceProfileVo {
    private final Long userId;
    private final long totalBehaviors;
    private final List<Map<String, Object>> topDestinations;
    private final List<Map<String, Object>> topCategories;
    private final List<String> topKeywords;
    private final Map<String, Object> preferenceSummary;
    private final String recommendationHint;
}
