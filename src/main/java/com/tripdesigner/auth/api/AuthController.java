package com.tripdesigner.auth.api;
/**
 * 认证 REST API 控制器。
 * 提供注册、登录、Token 刷新和登出功能。
 * 注册和登录接口无需认证，其余需 Bearer Token。
 */

import com.tripdesigner.auth.api.dto.LoginRequest;
import com.tripdesigner.auth.api.dto.RefreshRequest;
import com.tripdesigner.auth.api.dto.RegisterRequest;
import com.tripdesigner.auth.api.dto.TokenResponse;
import com.tripdesigner.auth.application.AuthAppService;
import com.tripdesigner.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "用户认证相关接口（注册、登录、Token 刷新、登出）")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthAppService authAppService;

    @Operation(summary = "用户注册", description = "注册新用户并返回 Access Token 与 Refresh Token")
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authAppService.register(req.getEmail(), req.getPassword()));
    }

    @Operation(summary = "用户登录", description = "验证邮箱密码，签发 Access Token 与 Refresh Token")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authAppService.login(req.getEmail(), req.getPassword()));
    }

    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 获取新的 Access Token，旧 Refresh Token 立即失效（Token 轮换）")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return Result.success(authAppService.refresh(req.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "吊销 Refresh Token，幂等操作")
    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authAppService.logout(req.getRefreshToken());
        return Result.success();
    }
}