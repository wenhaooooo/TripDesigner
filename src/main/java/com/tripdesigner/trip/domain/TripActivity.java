package com.tripdesigner.trip.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;

/**
 * 活动领域实体 —— 行程日中的具体活动安排。
 *
 * 是行程规划的最小粒度单元，代表一个具体的活动，
 * 如"参观东京塔"、"在新宿御苑吃午餐"等。
 *
 * category 字段支持分类：sightseeing, dining, transport,
 * accommodation, shopping, other
 */
@Getter
@Builder
public class TripActivity {

    private Long id;

    /** 所属行程日 ID */
    private Long tripDayId;

    /** 活动名称 */
    private String name;

    /** 活动描述 */
    private String description;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 活动分类（sightseeing/dining/transport/accommodation/shopping/other） */
    private String category;

    /** 地点 */
    private String place;

    /** 备注 */
    private String notes;

    /** 排序序号 */
    private Integer sortOrder;

    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    /**
     * 静态工厂方法：创建一个活动。
     *
     * @param tripDayId 所属行程日 ID
     * @param name      活动名称
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param category  活动分类
     * @param place     地点
     * @param notes     备注
     * @return 新的 TripActivity 实例
     */
    public static TripActivity create(Long tripDayId, String name, LocalTime startTime,
                                       LocalTime endTime, String category, String place, String notes) {
        return TripActivity.builder()
                .tripDayId(tripDayId)
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .category(category)
                .place(place)
                .notes(notes)
                .sortOrder(0)
                .version(0)
                .build();
    }

    /**
     * 更新字段（仅更新非 null 值）。
     *
     * @param name      新名称
     * @param startTime 新开始时间
     * @param endTime   新结束时间
     * @param category  新分类
     * @param place     新地点
     * @param notes     新备注
     * @param sortOrder 新排序
     * @return 更新后的新实例
     */
    public TripActivity withUpdatedFields(String name, LocalTime startTime, LocalTime endTime,
                                          String category, String place, String notes, Integer sortOrder) {
        return TripActivity.builder()
                .id(id).tripDayId(tripDayId)
                .name(name != null ? name : this.name)
                .startTime(startTime != null ? startTime : this.startTime)
                .endTime(endTime != null ? endTime : this.endTime)
                .category(category != null ? category : this.category)
                .place(place != null ? place : this.place)
                .notes(notes != null ? notes : this.notes)
                .sortOrder(sortOrder != null ? sortOrder : this.sortOrder)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
