package com.tripdesigner.price.domain;

/**
 * 价格监测类型枚举。
 * 表示监测的价格种类：机票、酒店、火车票。
 * 数据库中以字符串名称存储。
 */
public enum MonitorType {
    /** 机票 */
    FLIGHT,
    /** 酒店 */
    HOTEL,
    /** 火车票 */
    TRAIN
}
