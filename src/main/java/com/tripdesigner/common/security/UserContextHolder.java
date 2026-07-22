package com.tripdesigner.common.security;

/**
 * 用户上下文持有者（线程级）。
 * 使用 ThreadLocal 存储当前请求的用户上下文，
 * 确保在同一个请求处理链路中各处都可访问当前用户信息。
 *
 * 注意：在 filter 中设置，在 filter 结束时必须 clear()，
 * 防止内存泄漏和线程安全问题。
 */
public final class UserContextHolder {
    private static final ThreadLocal<UserContext> holder = new ThreadLocal<>();

    public static void set(UserContext ctx) { holder.set(ctx); }
    public static UserContext get() { return holder.get(); }
    public static void clear() { holder.remove(); }
}