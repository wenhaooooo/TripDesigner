package com.tripdesigner.price.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@TableName("train_ticket_info")
public class TrainTicketInfoPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("departure")
    private String departure;

    @TableField("destination")
    private String destination;

    @TableField("train_number")
    private String trainNumber;

    @TableField("ticket_class")
    private String ticketClass;

    @TableField("price")
    private BigDecimal price;

    @TableField("departure_time")
    private LocalTime departureTime;

    @TableField("arrival_time")
    private LocalTime arrivalTime;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("train_type")
    private String trainType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}