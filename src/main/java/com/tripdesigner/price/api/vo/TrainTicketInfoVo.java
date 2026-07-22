package com.tripdesigner.price.api.vo;

import com.tripdesigner.price.domain.TicketClass;
import com.tripdesigner.price.domain.TrainTicketInfo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
public class TrainTicketInfoVo {

    private final Long id;
    private final String departure;
    private final String destination;
    private final String trainNumber;
    private final String ticketClass;
    private final BigDecimal price;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final Integer durationMinutes;
    private final String trainType;

    public static TrainTicketInfoVo from(TrainTicketInfo info) {
        return TrainTicketInfoVo.builder()
                .id(info.getId())
                .departure(info.getDeparture())
                .destination(info.getDestination())
                .trainNumber(info.getTrainNumber())
                .ticketClass(info.getTicketClass().name())
                .price(info.getPrice())
                .departureTime(info.getDepartureTime())
                .arrivalTime(info.getArrivalTime())
                .durationMinutes(info.getDurationMinutes())
                .trainType(info.getTrainType())
                .build();
    }

    public String getDurationFormatted() {
        if (durationMinutes == null) return "";
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours > 0) {
            return hours + "h" + minutes + "m";
        }
        return minutes + "m";
    }

    public String getRoute() {
        return departure + " - " + destination;
    }

    public String getTicketClassLabel() {
        if (ticketClass == null) return "";
        return switch (ticketClass) {
            case "STANDING" -> "无座";
            case "HARD_SEAT" -> "硬座";
            case "HARD_BERTH" -> "硬卧";
            case "SOFT_BERTH" -> "软卧";
            case "SECOND_CLASS" -> "二等座";
            case "FIRST_CLASS" -> "一等座";
            case "BUSINESS_CLASS" -> "商务座";
            case "ECONOMY" -> "经济舱";
            case "BUSINESS" -> "商务舱";
            case "FIRST" -> "头等舱";
            default -> ticketClass;
        };
    }
}