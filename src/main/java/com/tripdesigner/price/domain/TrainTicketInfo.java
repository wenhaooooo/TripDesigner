package com.tripdesigner.price.domain;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalTime;

@Value
public class TrainTicketInfo {

    Long id;
    String departure;
    String destination;
    String trainNumber;
    TicketClass ticketClass;
    BigDecimal price;
    LocalTime departureTime;
    LocalTime arrivalTime;
    Integer durationMinutes;
    String trainType;

    public static TrainTicketInfo create(
            String departure,
            String destination,
            String trainNumber,
            TicketClass ticketClass,
            BigDecimal price,
            LocalTime departureTime,
            LocalTime arrivalTime,
            Integer durationMinutes,
            String trainType) {
        return new TrainTicketInfo(
                null,
                departure,
                destination,
                trainNumber,
                ticketClass,
                price,
                departureTime,
                arrivalTime,
                durationMinutes,
                trainType
        );
    }

    public TrainTicketInfo withId(Long id) {
        return new TrainTicketInfo(
                id,
                this.departure,
                this.destination,
                this.trainNumber,
                this.ticketClass,
                this.price,
                this.departureTime,
                this.arrivalTime,
                this.durationMinutes,
                this.trainType
        );
    }

    public String getDurationFormatted() {
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return minutes + "分钟";
    }

    public String getRoute() {
        return departure + " - " + destination;
    }
}