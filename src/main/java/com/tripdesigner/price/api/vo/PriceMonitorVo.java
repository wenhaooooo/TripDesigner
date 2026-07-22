package com.tripdesigner.price.api.vo;

import com.tripdesigner.price.domain.MonitorStatus;
import com.tripdesigner.price.domain.MonitorType;
import com.tripdesigner.price.domain.PriceMonitor;
import com.tripdesigner.price.domain.PricePoint;
import com.tripdesigner.price.domain.TicketClass;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/**
 * 价格监测视图对象（VO）。
 * 用于 REST API 响应，不暴露 version 字段。
 */
@Getter
@Builder
public class PriceMonitorVo {
    private final Long id;
    private final Long userId;
    private final Long tripId;
    private final String destination;
    private final String departure;
    private final String ticketClass;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final String monitorType;
    private final BigDecimal targetPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal lowestPrice;
    private final List<PricePoint> priceHistory;
    private final boolean notificationSent;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static PriceMonitorVo from(PriceMonitor m) {
        return PriceMonitorVo.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .tripId(m.getTripId())
                .destination(m.getDestination())
                .departure(m.getDeparture())
                .ticketClass(ticketClassName(m.getTicketClass()))
                .departureTime(m.getDepartureTime())
                .arrivalTime(m.getArrivalTime())
                .monitorType(monitorTypeName(m.getMonitorType()))
                .targetPrice(m.getTargetPrice())
                .currentPrice(m.getCurrentPrice())
                .lowestPrice(m.getLowestPrice())
                .priceHistory(m.getPriceHistory())
                .notificationSent(m.isNotificationSent())
                .status(monitorStatusName(m.getStatus()))
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private static String ticketClassName(TicketClass ticketClass) {
        return ticketClass != null ? ticketClass.name() : null;
    }

    private static String monitorTypeName(MonitorType type) {
        return type != null ? type.name() : null;
    }

    private static String monitorStatusName(MonitorStatus status) {
        return status != null ? status.name() : null;
    }
}
