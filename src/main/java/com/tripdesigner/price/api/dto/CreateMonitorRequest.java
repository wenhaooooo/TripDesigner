package com.tripdesigner.price.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 创建价格监测请求 DTO。
 * monitorType 默认为 FLIGHT；tripId 可选，用于关联具体行程。
 */
@Data
public class CreateMonitorRequest {

    @NotBlank(message = "destination is required")
    @Size(max = 255, message = "destination must be at most 255 characters")
    private String destination;

    /** 出发地（用于机票/火车票监测，如武汉-南京） */
    private String departure;

    /** 座位等级（用于机票/火车票，如 SECOND_CLASS、ECONOMY 等） */
    private String ticketClass;

    /** 发车/出发时间（用于机票/火车票） */
    private LocalTime departureTime;

    /** 到达时间（用于机票/火车票） */
    private LocalTime arrivalTime;

    /** 监测类型：FLIGHT / HOTEL / TRAIN，默认 FLIGHT */
    private String monitorType = "FLIGHT";

    /** 目标价格（低于此价格触发通知） */
    private BigDecimal targetPrice;

    /** 关联行程 ID（可选） */
    private Long tripId;
}
