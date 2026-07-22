package com.tripdesigner.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 限流过滤器。
 *
 * 对所有需要认证的接口按用户 ID 限流；匿名接口按客户端 IP 限流。
 * AI 工作流相关接口（/ai/workflow/**）使用更严格的限流策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        boolean allowed;

        // AI 工作流接口使用更严格的限流（每分钟 5 次）
        if (request.getRequestURI().startsWith("/ai/workflow/")) {
            allowed = rateLimiter.tryConsumeAi(key);
        } else {
            allowed = rateLimiter.tryConsume(key);
        }

        if (!allowed) {
            writeRateLimitExceeded(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        // 已登录用户按 userId 限流
        UserContext ctx = UserContextHolder.get();
        if (ctx != null && ctx.userId() != null) {
            return "user:" + ctx.userId();
        }
        // 匿名用户按 IP 限流
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }

    private void writeRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");
        Result<Void> body = Result.fail(ResultCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
