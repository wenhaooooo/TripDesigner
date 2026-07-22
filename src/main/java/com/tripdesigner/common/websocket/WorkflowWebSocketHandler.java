package com.tripdesigner.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流 WebSocket 处理器。
 *
 * 管理 sessionId → 已连接客户端集合 的映射，工作流完成时推送结果。
 * 端点：/ws/workflow/{sessionId}
 *
 * 支持同一 sessionId 多个并发连接（例如同一用户开多个浏览器 Tab），
 * 使用 Set<WebSocketSession> 避免旧连接被覆盖后泄漏（未关闭但仍占用资源）。
 */
@Slf4j
@Component
public class WorkflowWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        if (sessionId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        sessionMap.computeIfAbsent(sessionId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
        log.info("[WebSocket] Client connected for sessionId={}, total clients={}",
                sessionId, sessionMap.get(sessionId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session);
        if (sessionId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionMap.get(sessionId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionMap.remove(sessionId, sessions);
            }
            log.info("[WebSocket] Client disconnected for sessionId={}, status={}, remaining={}",
                    sessionId, status, sessions.size());
        }
    }

    /**
     * 推送结果给指定 sessionId 的所有已连接客户端。
     * 单个连接发送失败不影响其他连接。
     */
    public void sendResult(String sessionId, String message) {
        Set<WebSocketSession> sessions = sessionMap.get(sessionId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("[WebSocket] No active client for sessionId={}", sessionId);
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(message));
                log.info("[WebSocket] Result pushed to sessionId={}", sessionId);
            } catch (IOException e) {
                log.warn("[WebSocket] Failed to push result to sessionId={}, client={}",
                        sessionId, session.getId(), e);
            }
        }
    }

    /**
     * 从 URI 路径中提取 sessionId（最后一段路径）。
     * 例如 /ws/workflow/123 → "123"；/ws/workflow/123/ → "123"；/ws/workflow/123?token=xxx → "123"
     */
    private String extractSessionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        // 去掉末尾斜杠
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash == path.length() - 1) {
            return null;
        }
        String id = path.substring(slash + 1);
        return id.isEmpty() ? null : id;
    }
}
