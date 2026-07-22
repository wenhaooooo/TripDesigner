package com.tripdesigner.memory.infrastructure;
/**
 * 用户偏好数据库持久化对象。
 * 映射至 user_preferences 表。
 * data 字段存储 JSON 格式的偏好数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("user_preferences")
public class UserPreferencePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String category;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String data;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
}
