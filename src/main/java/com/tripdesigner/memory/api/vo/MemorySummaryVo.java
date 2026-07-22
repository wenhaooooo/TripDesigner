package com.tripdesigner.memory.api.vo;
/**
 * 偏好+记忆摘要视图对象。
 * 用于向 AI Agent 提供用户个性化信息的文本摘要。
 */

import lombok.Data;

@Data
public class MemorySummaryVo {
    private String preferenceSummary;
    private String tripMemorySummary;
}
