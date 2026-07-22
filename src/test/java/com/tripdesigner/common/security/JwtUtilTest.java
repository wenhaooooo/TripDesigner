package com.tripdesigner.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwt;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dummy-secret-must-be-at-least-256-bits-long-for-hs256-aaaaaaaa");
        props.setAccessTtlMinutes(15);
        props.setRefreshTtlDays(7);
        jwt = new JwtUtil(props);
    }

    @Test
    void access_token_roundtrip() {
        String token = jwt.generateAccessToken(42L, "a@b.com");
        Claims claims = jwt.parse(token);
        assertEquals(42L, claims.get("uid", Long.class));
        assertEquals("a@b.com", claims.getSubject());
        assertEquals("access", claims.get("type"));
    }

    @Test
    void refresh_token_has_refresh_type_and_7d_ttl() {
        String token = jwt.generateRefreshToken(42L);
        Claims claims = jwt.parse(token);
        assertEquals("refresh", claims.get("type"));
        assertEquals(42L, claims.get("uid", Long.class));
        assertTrue(jwt.refreshTtlSeconds() == 7 * 24 * 3600L);
    }

    @Test
    void isExpired_true_for_past_expiry() {
        String token = jwt.generateAccessToken(1L, "x@y.com");
        Claims claims = jwt.parse(token);
        assertFalse(jwt.isExpired(claims));
    }
}
