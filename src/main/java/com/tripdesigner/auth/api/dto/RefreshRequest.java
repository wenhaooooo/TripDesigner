package com.tripdesigner.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 刷新请求 DTO。
 * 使用 Refresh Token 换取新的 Access Token。
 */
@Data
public class RefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}