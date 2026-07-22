package com.tripdesigner.user.api.dto;
/**
 * 用户更新请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Size(max = 64) private String nickname;
}