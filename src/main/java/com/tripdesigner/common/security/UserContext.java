package com.tripdesigner.common.security;
/**
 * 用户上下文记录类。
 * 通过 JWT 解析后设置，在请求生命周期内可通过 UserContextHolder 获取。
 * 包含当前用户 ID 和邮箱信息，供业务层进行权限校验。
 */

public record UserContext(Long userId, String email) {
}
