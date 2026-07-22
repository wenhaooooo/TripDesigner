package com.tripdesigner.auth.application;

import com.tripdesigner.auth.api.dto.TokenResponse;
import com.tripdesigner.auth.domain.RefreshToken;
import com.tripdesigner.auth.domain.RefreshTokenRepository;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.JwtUtil;
import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用服务。
 *
 * 处理用户注册、登录、Token 刷新和登出的核心业务逻辑。
 * 使用 JWT 双令牌机制（Access Token + Refresh Token），
 * 提供 Token 轮换（rotation）功能防止 Refresh Token 被盗用。
 *
 * 安全设计：
 * - 注册时预检邮箱唯一性，并发兜底使用 DataIntegrityViolationException
 * - 登录时验证密码哈希（BCrypt）
 * - Refresh Token 使用 SHA-256 哈希存储在 Redis 中
 * - 每次 refresh 操作轮换 Token（旧令牌立即失效）
 */
@Service
public class AuthAppService {
    private final UserRepository userRepo;
    private final JwtUtil jwt;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthAppService(UserRepository userRepo, JwtUtil jwt,
                          RefreshTokenRepository refreshRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.jwt = jwt;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse register(String email, String password) {
        if (userRepo.existsByEmail(email)) {
            throw new BizException(ResultCode.USER_EMAIL_EXISTS);
        }
        try {
            User user = userRepo.save(User.register(email, passwordEncoder.encode(password)));
            return issueTokens(user);
        } catch (DataIntegrityViolationException e) {
            // 兜底竞态：预检后并发注册
            throw new BizException(ResultCode.USER_EMAIL_EXISTS);
        }
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BizException(ResultCode.AUTH_INVALID_CREDENTIALS));
        if (!user.isActive()) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ResultCode.AUTH_INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwt.parse(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "not a refresh token");
        }
        Long userId = claims.get("uid", Long.class);
        // tokenId 始终使用 refresh token 哈希前缀，不依赖 JWT jti
        String tokenId = RefreshToken.hash(refreshToken).substring(0, 16);

        String stored = refreshRepo.findHash(userId, tokenId)
                .orElseThrow(() -> new BizException(ResultCode.AUTH_REFRESH_REVOKED));
        if (!stored.equals(RefreshToken.hash(refreshToken))) {
            // token 内容与存储不符：可能被盗用，吊销
            refreshRepo.revoke(userId, tokenId);
            throw new BizException(ResultCode.AUTH_REFRESH_REVOKED);
        }
        // 轮换：删旧
        refreshRepo.revoke(userId, tokenId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        try {
            Claims claims = jwt.parse(refreshToken);
            Long userId = claims.get("uid", Long.class);
            String tokenId = RefreshToken.hash(refreshToken).substring(0, 16);
            refreshRepo.revoke(userId, tokenId);
        } catch (BizException ignored) {
            // 无效 token 登出：幂等，无操作
        }
    }

    private TokenResponse issueTokens(User user) {
        String access = jwt.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwt.generateRefreshToken(user.getId());
        // tokenId 取 token 哈希前缀作为 Redis key，不依赖 JWT jti
        String tid = RefreshToken.hash(refresh).substring(0, 16);
        RefreshToken rt = RefreshToken.builder()
                .tokenId(tid)
                .userId(user.getId())
                .tokenHash(RefreshToken.hash(refresh))
                .build();
        refreshRepo.save(rt, jwt.refreshTtlSeconds());

        return TokenResponse.of(access, refresh, jwt.getAccessTtlSeconds());
    }
}