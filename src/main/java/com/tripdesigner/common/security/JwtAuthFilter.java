package com.tripdesigner.common.security;
/**
 * JWT 认证过滤器。
 * 从 Authorization Header 提取 Bearer Token，
 * 解析 JWT 并设置用户上下文和 Spring Security 认证信息。
 *
 * 执行流程：
 * 1. 提取 Authorization: Bearer xxx
 * 2. 解析 JWT 验证签名
 * 3. 校验 type=access
 * 4. 设置 UserContext 到 UserContextHolder
 * 5. 设置 UsernamePasswordAuthenticationToken 到 SecurityContext
 * 6. 清理上下文（finally 块中）
 */

import io.jsonwebtoken.Claims;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String BEARER = "Bearer ";
    private final JwtUtil jwt;

    public JwtAuthFilter(JwtUtil jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getDispatcherType() == DispatcherType.ERROR || req.getDispatcherType() == DispatcherType.ASYNC) {
            chain.doFilter(req, res);
            return;
        }
        String header = req.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length());
            try {
                Claims claims = jwt.parse(token);
                if (!"access".equals(claims.get("type"))) {
                    chain.doFilter(req, res);
                    return;
                }
                Long userId = claims.get("uid", Long.class);
                String email = claims.getSubject();
                UserContext ctx = new UserContext(userId, email);
                UserContextHolder.set(ctx);
                MDC.put("userId", String.valueOf(userId));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        ctx, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // invalid token: let Security handle rejection
            }
        }
        try {
            chain.doFilter(req, res);
        } finally {
            UserContextHolder.clear();
            MDC.remove("userId");
        }
    }
}