package com.tripdesigner.price.domain;

/**
 * 价格监测状态枚举。
 *
 * ACTIVE → TRIGGERED（达到目标价格）
 * ACTIVE → CANCELLED（用户主动取消）
 * ACTIVE → EXPIRED（监测过期）
 *
 * 数据库中以字符串名称存储。
 */
public enum MonitorStatus {
    /** 监测中 */
    ACTIVE,
    /** 已触发目标价格 */
    TRIGGERED,
    /** 已过期 */
    EXPIRED,
    /** 已取消 */
    CANCELLED
}
