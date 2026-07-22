package com.tripdesigner.trip.domain;

/**
 * 分享状态枚举。
 *
 * ACTIVE：有效
 * EXPIRED：已过期（到达 expires_at 时间或访问次数耗尽）
 * REVOKED：已撤销（owner 主动撤销）
 *
 * 以字符串形式存储在数据库 trip_shares.status 字段中。
 */
public enum ShareStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
