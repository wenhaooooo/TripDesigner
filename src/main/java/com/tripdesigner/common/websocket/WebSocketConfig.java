package com.tripdesigner.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 *
 * 注册 /ws/workflow/{sessionId} 端点，前端连接后保持长连接，
 * 工作流完成时由 WorkflowResultListener 推送结果。
 *
 * 同时支持轮询（GET /ai/workflow/{sessionId}），WebSocket 是优化项。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WorkflowWebSocketHandler workflowWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(workflowWebSocketHandler, "/ws/workflow/{sessionId}")
                .setAllowedOrigins("*");
    }
}
