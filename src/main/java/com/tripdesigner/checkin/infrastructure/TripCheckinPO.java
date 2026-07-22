package com.tripdesigner.checkin.infrastructure;

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

@Data
@TableName(value = "trip_checkins", autoResultMap = true)
public class TripCheckinPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tripId;
    private Long tripDayId;
    private Long activityId;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String photoUrls;
    private String status;
    private Instant checkedInAt;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
