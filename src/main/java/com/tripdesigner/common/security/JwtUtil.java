package com.tripdesigner.common.security;
/**
 * JWT 工具类。
 * 负责生成和解析 JWT 令牌（Access Token 和 Refresh Token）。
 * 使用 HMAC-SHA256 签名算法，密钥从配置读取。
 *
 * Access Token: 包含 userId(uid) 和 type=access，有效期短
 * Refresh Token: 包含 userId(uid) 和 type=refresh，有效期长
 */

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String TYPE = "type";
    private static final String UID = "uid";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtUtil(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email) {
        return build(userId, email, ACCESS, Duration.ofMinutes(props.getAccessTtlMinutes()));
    }

    public String generateRefreshToken(Long userId) {
        return build(userId, null, REFRESH, Duration.ofDays(props.getRefreshTtlDays()));
    }

    private String build(Long userId, String subject, String type, Duration ttl) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject == null ? String.valueOf(userId) : subject)
                .claim(UID, userId)
                .claim(TYPE, type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new com.tripdesigner.common.exception.BizException(
                    com.tripdesigner.common.response.ResultCode.AUTH_TOKEN_INVALID, e.getMessage());
        }
    }

    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public long refreshTtlSeconds() {
        return Duration.ofDays(props.getRefreshTtlDays()).toSeconds();
    }

    public long getAccessTtlSeconds() {
        return Duration.ofMinutes(props.getAccessTtlMinutes()).toSeconds();
    }
}
