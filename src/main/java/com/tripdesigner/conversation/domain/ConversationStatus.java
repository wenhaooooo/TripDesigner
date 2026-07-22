package com.tripdesigner.conversation.domain;

import lombok.Getter;

/**
 * 对话状态枚举。
 * 状态码以 SMALLINT 类型存储在数据库中。
 * 0=ACTIVE, 1=ARCHIVED, 2=DELETED
 */
@Getter
public enum ConversationStatus {
    ACTIVE((short) 0),
    ARCHIVED((short) 1),
    DELETED((short) 2);

    private final short code;

    ConversationStatus(short code) {
        this.code = code;
    }

    /**
     * 从数据库状态码解析枚举。
     *
     * @param code 状态码（SMALLINT）
     * @return 对应的枚举值（未匹配则默认 ACTIVE）
     */
    public static ConversationStatus of(short code) {
        for (ConversationStatus s : values()) {
            if (s.code == code) return s;
        }
        return ACTIVE;
    }
}