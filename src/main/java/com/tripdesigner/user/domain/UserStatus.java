package com.tripdesigner.user.domain;

import lombok.Getter;

/**
 * 用户状态枚举。
 * 状态码以 SMALLINT 类型存储在数据库中。
 */
@Getter
public enum UserStatus {
    /** 已禁用 */
    DISABLED((short) 0),
    /** 正常 */
    ACTIVE((short) 1);

    private final short code;

    UserStatus(short code) {
        this.code = code;
    }

    /**
     * 从数据库状态码解析枚举。
     *
     * @param code 状态码（SMALLINT）
     * @return 对应的枚举值（未匹配则默认 ACTIVE）
     */
    public static UserStatus of(short code) {
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        return ACTIVE;
    }
}