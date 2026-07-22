package com.tripdesigner.price.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * 价格监测数据库持久化对象。
 * 映射至 price_monitors 表。
 * price_history 字段使用 JSONB 类型存储价格历史列表（JSON 字符串）。
 * status、monitor_type 字段使用字符串名称存储对应枚举值。
 */
@Data
@TableName(value = "price_monitors", autoResultMap = true)
public class PriceMonitorPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tripId;
    private String destination;
    private String departure;
    private String ticketClass;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String monitorType;
    private BigDecimal targetPrice;
    private BigDecimal currentPrice;
    private BigDecimal lowestPrice;
    /** JSONB：以 JSON 字符串形式存储 List<PricePoint> */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String priceHistory;
    private Boolean notificationSent;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
