package com.tripdesigner.user.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户领域实体。
 * 代表系统中的一个注册用户，包含登录凭据和基本信息。
 */
@Getter
@Builder
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static User register(String email, String passwordHash) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .version(0)
                .build();
    }

    public User withNickname(String nickname) {
        return User.builder()
                .id(id).email(email).passwordHash(passwordHash)
                .nickname(nickname).status(status).createdAt(createdAt)
                .updatedAt(updatedAt).version(version)
                .build();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}