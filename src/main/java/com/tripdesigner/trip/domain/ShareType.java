package com.tripdesigner.trip.domain;

/**
 * 分享类型枚举。
 *
 * VIEW：仅查看
 * EDIT：可编辑（预留扩展）
 *
 * 以字符串形式存储在数据库 trip_shares.share_type 字段中。
 */
public enum ShareType {
    VIEW,
    EDIT
}
