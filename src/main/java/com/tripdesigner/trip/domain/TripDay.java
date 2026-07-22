package com.tripdesigner.trip.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 行程日领域实体 —— 行程中的一个具体日期。
 *
 * 属于 Trip（行程）的一个部分，是行程安排的基本时间单位。
 * 一个 TripDay 关联多个 TripActivity（活动）。
 *
 * 数据层次：Trip → TripDay → TripActivity
 */
@Getter
@Builder
public class TripDay {

    /** 主键 ID */
    private Long id;

    /** 所属行程 ID */
    private Long tripId;

    /** 第几天（如 1, 2, 3...） */
    private Integer dayNumber;

    /** 日期 */
    private LocalDate date;

    /** 当日主题（如"东京探索日"、"京都文化之旅"） */
    private String title;

    /** 当日描述 */
    private String description;

    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    /**
     * 静态工厂方法：创建一个行程日。
     *
     * @param tripId      所属行程 ID
     * @param dayNumber   第几天
     * @param date        日期
     * @param title       主题标题
     * @param description 描述
     * @return 新的 TripDay 实例
     */
    public static TripDay create(Long tripId, Integer dayNumber, LocalDate date,
                                 String title, String description) {
        return TripDay.builder()
                .tripId(tripId)
                .dayNumber(dayNumber)
                .date(date)
                .title(title)
                .description(description)
                .version(0)
                .build();
    }

    /**
     * 更新字段（仅更新非 null 值）。
     *
     * @param date        新日期
     * @param title       新标题
     * @param description 新描述
     * @return 更新后的新实例
     */
    public TripDay withUpdatedFields(LocalDate date, String title, String description) {
        return TripDay.builder()
                .id(id).tripId(tripId).dayNumber(dayNumber)
                .date(date != null ? date : this.date)
                .title(title != null ? title : this.title)
                .description(description != null ? description : this.description)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
