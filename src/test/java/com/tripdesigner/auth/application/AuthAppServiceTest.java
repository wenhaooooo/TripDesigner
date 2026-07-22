package com.tripdesigner.auth.application;

import com.tripdesigner.auth.api.dto.TokenResponse;
import com.tripdesigner.auth.domain.RefreshToken;
import com.tripdesigner.auth.domain.RefreshTokenRepository;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.JwtProperties;
import com.tripdesigner.common.security.JwtUtil;
import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthAppServiceTest {

    private UserRepository userRepo;
    private RefreshTokenRepository refreshRepo;
    private JwtUtil jwt;
    private AuthAppService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        refreshRepo = mock(RefreshTokenRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        JwtProperties props = new JwtProperties();
        props.setSecret("dummy-secret-must-be-at-least-256-bits-long-for-hs256-aaaaaaaa");
        props.setAccessTtlMinutes(15);
        props.setRefreshTtlDays(7);
        jwt = new JwtUtil(props);
        service = new AuthAppService(userRepo, jwt, refreshRepo, encoder);
    }

    @Test
    void register_creates_user_and_returns_tokens() {
        when(userRepo.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder().id(1L).email(u.getEmail()).passwordHash(u.getPasswordHash())
                    .status(u.getStatus()).version(0).build();
        });

        TokenResponse t = service.register("a@b.com", "password123");

        assertNotNull(t.getAccessToken());
        assertNotNull(t.getRefreshToken());
        assertEquals(900, t.getExpiresIn());
        verify(refreshRepo).save(any(RefreshToken.class), anyLong());
    }

    @Test
    void register_rejects_existing_email() {
        when(userRepo.existsByEmail("a@b.com")).thenReturn(true);
        BizException ex = assertThrows(BizException.class, () -> service.register("a@b.com", "pw"));
        assertEquals(ResultCode.USER_EMAIL_EXISTS, ex.getResultCode());
    }

    @Test
    void login_succeeds_with_correct_password() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode("password123");
        User u = User.builder().id(1L).email("a@b.com").passwordHash(hash).status(UserStatus_ACTIVE()).build();
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        TokenResponse t = service.login("a@b.com", "password123");
        assertNotNull(t.getAccessToken());
        verify(refreshRepo).save(any(), anyLong());
    }

    @Test
    void login_fails_with_wrong_password() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        User u = User.builder().id(1L).email("a@b.com").passwordHash(enc.encode("right")).status(UserStatus_ACTIVE()).build();
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        BizException ex = assertThrows(BizException.class, () -> service.login("a@b.com", "wrong"));
        assertEquals(ResultCode.AUTH_INVALID_CREDENTIALS, ex.getResultCode());
    }

    @Test
    void refresh_rotates_and_revokes_old() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(
                User.builder().id(2L).email("a@b.com").status(UserStatus_ACTIVE()).build()));
        String oldRefresh = jwt.generateRefreshToken(1L);
        // 模拟 Redis 中存在该 token
        when(refreshRepo.findHash(eq(1L), anyString()))
                .thenReturn(Optional.of(RefreshToken.hash(oldRefresh)));

        TokenResponse t = service.refresh(oldRefresh);

        assertNotNull(t.getRefreshToken());
        assertNotEquals(oldRefresh, t.getRefreshToken());
        verify(refreshRepo).revoke(eq(1L), anyString());
        verify(refreshRepo, times(1)).save(any(), anyLong());
    }

    @Test
    void refresh_revoked_token_throws() {
        String oldRefresh = jwt.generateRefreshToken(1L);
        when(refreshRepo.findHash(eq(1L), anyString())).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.refresh(oldRefresh));
        assertEquals(ResultCode.AUTH_REFRESH_REVOKED, ex.getResultCode());
    }

    @Test
    void logout_revokes_token() {
        String refresh = jwt.generateRefreshToken(1L);
        service.logout(refresh);
        verify(refreshRepo).revoke(eq(1L), anyString());
    }

    // helper
    private com.tripdesigner.user.domain.UserStatus UserStatus_ACTIVE() {
        return com.tripdesigner.user.domain.UserStatus.ACTIVE;
    }
}