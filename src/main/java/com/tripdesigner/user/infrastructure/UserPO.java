package com.tripdesigner.user.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 用户数据库持久化对象。
 * 映射至 users 表。
 * status 字段使用 SMALLINT 存储 UserStatus 的状态码。
 */
@Data
@TableName("users")
public class UserPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private Short status;       // SMALLINT: UserStatus.code
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}