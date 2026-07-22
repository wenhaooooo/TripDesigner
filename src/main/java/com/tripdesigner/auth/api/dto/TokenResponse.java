package com.tripdesigner.auth.api.dto;

import lombok.Getter;

/**
 * 令牌响应 DTO。
 * 包含 Access Token（短期）和 Refresh Token（长期），
 * 以及 Access Token 的有效期（秒）。
 */
@Getter
public class TokenResponse {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;

    private TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn);
    }
}