package com.tripdesigner.user.api.vo;
/**
 * 用户视图对象（VO）。
 */

import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserVo {
    private final Long id;
    private final String email;
    private final String nickname;
    private final String status;
    private final Instant createdAt;

    public static UserVo from(User u) {
        return UserVo.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nickname(u.getNickname())
                .status(u.getStatus() == null ? null : u.getStatus().name())
                .createdAt(u.getCreatedAt())
                .build();
    }
}