package com.tripdesigner.price.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 价格监测领域实体 —— 核心聚合根。
 *
 * 代表用户对某一目的地特定类型（机票/酒店/火车票）的价格监测任务。
 * 当价格降至目标价格以下时，触发通知。
 *
 * 使用不可变对象模式：
 * - 所有字段通过 @Getter 只读
 * - 状态变更通过 withXxx() 方法返回新实例
 * - 通过静态工厂方法 create() 创建
 */
@Getter
@Builder
public class PriceMonitor {

    /** 主键 ID */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 关联行程 ID（可选） */
    private Long tripId;

    /** 目的地 */
    private String destination;

    /** 出发地（用于机票/火车票监测，如武汉-南京） */
    private String departure;

    /** 座位等级（用于机票/火车票，如二等座、经济舱等） */
    private TicketClass ticketClass;

    /** 发车/出发时间（用于机票/火车票） */
    private LocalTime departureTime;

    /** 到达时间（用于机票/火车票） */
    private LocalTime arrivalTime;

    /** 监测类型 */
    private MonitorType monitorType;

    /** 目标价格（低于此价格则触发通知） */
    private BigDecimal targetPrice;

    /** 当前价格 */
    private BigDecimal currentPrice;

    /** 历史最低价 */
    private BigDecimal lowestPrice;

    /** 价格历史记录 */
    private List<PricePoint> priceHistory;

    /** 是否已发送过通知 */
    private boolean notificationSent;

    /** 监测状态 */
    private MonitorStatus status;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;

    /**
     * 静态工厂方法：创建一个新的价格监测。
     * 初始状态为 ACTIVE，版本号为 0，价格历史为空列表。
     *
     * @param userId       用户 ID
     * @param tripId       关联行程 ID（可空）
     * @param destination  目的地
     * @param monitorType  监测类型
     * @param targetPrice  目标价格
     * @return 新的 PriceMonitor 实例
     */
    public static PriceMonitor create(Long userId, Long tripId, String destination, String departure,
                                      MonitorType monitorType, TicketClass ticketClass,
                                      LocalTime departureTime, LocalTime arrivalTime,
                                      BigDecimal targetPrice) {
        return PriceMonitor.builder()
                .userId(userId)
                .tripId(tripId)
                .destination(destination)
                .departure(departure)
                .ticketClass(ticketClass)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .monitorType(monitorType)
                .targetPrice(targetPrice)
                .priceHistory(new ArrayList<>())
                .notificationSent(false)
                .status(MonitorStatus.ACTIVE)
                .version(0)
                .build();
    }

    /**
     * 添加价格点，并更新当前价格和历史最低价。
     * 返回新的实例（不可变模式）。
     *
     * @param point 价格点
     * @return 更新后的新实例
     */
    public PriceMonitor addPricePoint(PricePoint point) {
        List<PricePoint> newHistory = new ArrayList<>(this.priceHistory != null ? this.priceHistory : List.of());
        newHistory.add(point);
        BigDecimal newCurrent = point.getPrice();
        BigDecimal newLowest = this.lowestPrice == null || newCurrent.compareTo(this.lowestPrice) < 0
                ? newCurrent : this.lowestPrice;
        return PriceMonitor.builder()
                .id(id).userId(userId).tripId(tripId).destination(destination).departure(departure)
                .ticketClass(ticketClass).departureTime(departureTime).arrivalTime(arrivalTime)
                .monitorType(monitorType).targetPrice(targetPrice)
                .currentPrice(newCurrent).lowestPrice(newLowest)
                .priceHistory(newHistory)
                .notificationSent(notificationSent).status(status)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    /**
     * 更新价格：创建新的价格点并添加到历史记录。
     *
     * @param newPrice 新价格
     * @param source   数据来源
     * @return 更新后的新实例
     */
    public PriceMonitor withUpdatedPrice(BigDecimal newPrice, String source) {
        return addPricePoint(PricePoint.builder()
                .price(newPrice)
                .recordedAt(Instant.now())
                .source(source)
                .build());
    }

    /**
     * 更新监测状态。
     *
     * @param newStatus 新状态
     * @return 更新状态后的新实例
     */
    public PriceMonitor withStatus(MonitorStatus newStatus) {
        return PriceMonitor.builder()
                .id(id).userId(userId).tripId(tripId).destination(destination).departure(departure)
                .ticketClass(ticketClass).departureTime(departureTime).arrivalTime(arrivalTime)
                .monitorType(monitorType).targetPrice(targetPrice)
                .currentPrice(currentPrice).lowestPrice(lowestPrice)
                .priceHistory(priceHistory)
                .notificationSent(notificationSent).status(newStatus)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    /**
     * 判断是否应当发送价格通知。
     * 条件：已设置目标价格和当前价格，当前价格 <= 目标价格，且尚未通知过。
     *
     * @return 是否应当通知
     */
    public boolean shouldNotify() {
        return targetPrice != null
                && currentPrice != null
                && currentPrice.compareTo(targetPrice) <= 0
                && !notificationSent;
    }
}
