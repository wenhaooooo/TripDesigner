package com.tripdesigner.common.security;
/**
 * Spring Security 安全配置。
 * - 禁用 CSRF（API 使用 Bearer Token 认证）
 * - 无状态会话（STATELESS）
 * - 配置免认证路径（permitPaths）
 * - 添加 JWT 认证过滤器
 * - 限流过滤器（RateLimitFilter）放在 JWT 之后，按用户 ID/IP 限流
 * - 统一处理认证失败和权限异常
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.ratelimit.RateLimitFilter;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper;
    private final List<String> permitPaths;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter, ObjectMapper objectMapper,
                          @Value("${app.auth.permit-paths}") String permitPathsCsv) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.objectMapper = objectMapper;
        this.permitPaths = List.of(permitPathsCsv.split(","));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] permit = permitPaths.toArray(new String[0]);
        http
                .securityMatcher(req -> req.getDispatcherType() == jakarta.servlet.DispatcherType.REQUEST)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permit).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeJson(res, HttpServletResponse.SC_UNAUTHORIZED,
                                Result.fail(ResultCode.AUTH_TOKEN_INVALID)))
                        .accessDeniedHandler((req, res, ex) -> writeJson(res, HttpServletResponse.SC_FORBIDDEN,
                                Result.fail(ResultCode.AUTH_TOKEN_INVALID, "access denied"))))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse res, int status, Result<Void> body) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}