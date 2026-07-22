package com.tripdesigner.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性。
 * 从 application.yml 的 jwt.* 读取配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** JWT 签名密钥 */
    private String secret;

    /** Access Token 有效期（分钟） */
    private int accessTtlMinutes = 30;

    /** Refresh Token 有效期（天） */
    private int refreshTtlDays = 7;
}