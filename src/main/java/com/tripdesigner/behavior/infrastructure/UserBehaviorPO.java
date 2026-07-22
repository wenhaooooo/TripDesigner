package com.tripdesigner.behavior.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName(value = "user_behaviors", autoResultMap = true)
public class UserBehaviorPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String behaviorType;
    private String targetType;
    private Long targetId;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String context;
    private Integer weight;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
