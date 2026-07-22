package com.tripdesigner.memory.domain;

/**
 * 偏好来源枚举。
 * 标识偏好是由用户手动设置还是 AI 自动发现的。
 */
public enum PreferenceSource {
    /** 用户手动设置 */
    MANUAL,
    /** AI 从对话中自动发现 */
    AI_DISCOVERED
}