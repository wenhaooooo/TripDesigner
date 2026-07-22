package com.tripdesigner.price.domain;

/**
 * 座位等级枚举。
 * 用于机票、火车票的价格监测，不同等级价格不同。
 * 数据库中以字符串名称存储。
 */
public enum TicketClass {

    /** 无座/站票 */
    STANDING,

    /** 硬座（普速列车） */
    HARD_SEAT,

    /** 硬卧（普速列车） */
    HARD_BERTH,

    /** 软卧（普速列车） */
    SOFT_BERTH,

    /** 二等座（高铁/动车） */
    SECOND_CLASS,

    /** 一等座（高铁/动车） */
    FIRST_CLASS,

    /** 商务座（高铁/动车） */
    BUSINESS_CLASS,

    /** 经济舱（机票） */
    ECONOMY,

    /** 商务舱（机票） */
    BUSINESS,

    /** 头等舱（机票） */
    FIRST;

    public static TicketClass fromValue(String value) {
        if (value == null) return null;
        try {
            return TicketClass.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}