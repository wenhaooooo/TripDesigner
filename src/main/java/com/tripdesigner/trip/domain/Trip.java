package com.tripdesigner.trip.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 行程领域实体 —— 核心聚合根。
 *
 * 代表一次完整的旅行计划，是系统的核心业务对象。
 * 一个 Trip 包含多个 TripDay（行程日），
 * 每个 TripDay 又包含多个 TripActivity（活动）。
 *
 * 使用不可变对象模式：
 * - 所有字段通过 @Getter 只读
 * - 状态变更通过 withXxx() 方法返回新实例
 * - 通过静态工厂方法 Trip.create() 创建
 */
@Getter
@Builder
public class Trip {

    /** 主键 ID */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 行程标题 */
    private String title;

    /** 行程描述 */
    private String description;

    /** 目的地名称 */
    private String destinationName;

    /** 行程状态 */
    private TripStatus status;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 预算（当地货币单位） */
    private Integer budget;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;

    /**
     * 静态工厂方法：创建一个新的行程。
     * 初始状态为 DRAFT，版本号为 0。
     *
     * @param userId          用户 ID
     * @param title           标题
     * @param description     描述
     * @param destinationName 目的地
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param budget          预算
     * @return 新的 Trip 实例
     */
    public static Trip create(Long userId, String title, String description, String destinationName,
                              LocalDate startDate, LocalDate endDate, Integer budget) {
        return Trip.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .destinationName(destinationName)
                .startDate(startDate)
                .endDate(endDate)
                .budget(budget)
                .status(TripStatus.DRAFT)
                .version(0)
                .build();
    }

    /**
     * 生成新的 Trip 实例，仅变更状态字段。
     *
     * @param status 新状态
     * @return 更新了状态的新实例
     */
    public Trip withUpdatedStatus(TripStatus status) {
        return Trip.builder()
                .id(id).userId(userId).title(title).description(description)
                .destinationName(destinationName).status(status)
                .startDate(startDate).endDate(endDate).budget(budget)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    /**
     * 生成新的 Trip 实例，更新可编辑字段。
     * 仅更新非 null 的字段，保持原有值不变。
     *
     * @param title           新标题（null 忽略）
     * @param description     新描述（null 忽略）
     * @param destinationName 新目的地（null 忽略）
     * @param startDate       新开始日期（null 忽略）
     * @param endDate         新结束日期（null 忽略）
     * @param budget          新预算（null 忽略）
     * @return 更新了字段的新实例
     */
    public Trip withUpdatedFields(String title, String description, String destinationName,
                                  LocalDate startDate, LocalDate endDate, Integer budget) {
        return Trip.builder()
                .id(id).userId(userId)
                .title(title != null ? title : this.title)
                .description(description != null ? description : this.description)
                .destinationName(destinationName != null ? destinationName : this.destinationName)
                .status(status)
                .startDate(startDate != null ? startDate : this.startDate)
                .endDate(endDate != null ? endDate : this.endDate)
                .budget(budget != null ? budget : this.budget)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
