package com.tripdesigner.common.response;

import lombok.Getter;

/**
 * 统一业务状态码枚举。
 * 定义系统中所有可能的业务响应状态码和消息。
 * 格式：模块_具体错误，如 AUTH_TOKEN_INVALID。
 */
@Getter
public enum ResultCode {

    // ========== 通用 ==========
    SUCCESS(0, "success"),
    COMMON_BAD_REQUEST(400, "bad request"),
    COMMON_INTERNAL_ERROR(500, "internal error"),
    COMMON_NOT_FOUND(404, "not found"),
    TOO_MANY_REQUESTS(429, "too many requests"),

    // ========== 认证 ==========
    AUTH_TOKEN_INVALID(1001, "invalid token"),
    AUTH_REFRESH_REVOKED(1002, "refresh token revoked"),
    AUTH_INVALID_CREDENTIALS(1003, "invalid credentials"),

    // ========== 用户 ==========
    USER_EMAIL_EXISTS(2001, "email already exists"),
    USER_NOT_FOUND(2002, "user not found"),
    USER_DISABLED(2003, "user disabled"),

    // ========== 行程 ==========
    TRIP_NOT_FOUND(3001, "trip not found"),
    TRIP_NOT_OWNER(3002, "trip does not belong to user"),
    TRIP_DAY_NOT_FOUND(3003, "trip day not found"),
    TRIP_DAY_NOT_OWNER(3004, "trip day does not belong to user"),
    TRIP_ACTIVITY_NOT_FOUND(3005, "activity not found"),
    DESTINATION_NOT_FOUND(3006, "destination not found"),

    // ========== 对话 ==========
    CONV_NOT_FOUND(4001, "conversation not found"),
    CONV_NOT_OWNER(4002, "conversation does not belong to user"),

    // ========== AI ==========
    AI_GENERATION_FAILED(5001, "AI generation failed"),

    // ========== 体验 ==========
    EXPERIENCE_NOT_FOUND(6001, "experience not found"),

    // ========== 偏好 ==========
    PREFERENCE_NOT_FOUND(7001, "preference not found"),

    // ========== 权限 ==========
    PERMISSION_DENIED(8001, "permission denied");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}