package com.tripdesigner.common.health;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * AI 服务健康检查。
 *
 * 检查 ChatClient 是否可用，避免健康检查频繁调用 LLM API 消耗 token。
 * 仅验证 Bean 是否正确注入，实际 API 可用性由 /ai/smoke 端点手动验证。
 */
@Component
public class AiServiceHealthIndicator implements HealthIndicator {

    private final ChatClient chatClient;

    public AiServiceHealthIndicator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Health health() {
        if (chatClient == null) {
            return Health.down().withDetail("error", "ChatClient not configured").build();
        }
        return Health.up()
                .withDetail("component", "spring-ai-chat-client")
                .build();
    }
}
