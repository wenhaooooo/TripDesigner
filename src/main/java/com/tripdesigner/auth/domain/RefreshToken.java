package com.tripdesigner.auth.domain;

import lombok.Builder;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * 刷新令牌领域实体。
 * 使用 SHA-256 哈希存储令牌内容，不存储明文。
 * tokenId 为哈希前 16 位，用作 Redis key 的一部分。
 */
@Getter
@Builder
public class RefreshToken {
    private String tokenId;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;

    /**
     * 对令牌进行 SHA-256 哈希。
     * 用于安全存储和比对，防止令牌泄露导致凭据被盗。
     */
    public static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}