package com.tripdesigner.trip.domain;

import lombok.Getter;

/**
 * 行程状态枚举。
 *
 * DRAFT → PLANNED → ACTIVE → COMPLETED（正常流程）
 *                      ↘ CANCELLED（取消）
 *
 * 状态码以 short 类型存储在数据库中，
 * 通过 of() 工厂方法从代码值解析枚举。
 */
@Getter
public enum TripStatus {

    /** 草稿：行程刚创建，尚未规划 */
    DRAFT(0),

    /** 规划中：正在添加行程日和活动 */
    PLANNED(1),

    /** 进行中：行程已确认，正在执行 */
    ACTIVE(2),

    /** 已取消 */
    CANCELLED(3),

    /** 已完成：行程结束 */
    COMPLETED(4);

    /** 数据库存储的状态码 */
    private final short code;

    TripStatus(int code) {
        this.code = (short) code;
    }

    /**
     * 从整数代码值解析枚举。
     * 用于从数据库读取后的反序列化。
     *
     * @param code 状态码
     * @return 对应的枚举值（未匹配则默认 DRAFT）
     */
    public static TripStatus of(int code) {
        for (TripStatus s : values()) {
            if (s.code == code) return s;
        }
        return DRAFT;
    }
}
