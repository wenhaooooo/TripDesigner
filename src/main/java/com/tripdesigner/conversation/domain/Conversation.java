package com.tripdesigner.conversation.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 对话领域实体。
 * 代表用户与 AI 之间的一次对话会话，
 * 包含多条 ConversationMessage。
 */
@Getter
@Builder
public class Conversation {
    private Long id;
    private Long userId;
    private String title;
    private ConversationStatus status;
    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static Conversation create(Long userId, String title) {
        return Conversation.builder()
                .userId(userId)
                .title(title != null ? title : "New Conversation")
                .status(ConversationStatus.ACTIVE)
                .version(0)
                .build();
    }

    public Conversation withUpdatedTitle(String title) {
        return Conversation.builder()
                .id(id).userId(userId)
                .title(title)
                .status(status).lastMessageAt(lastMessageAt)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public Conversation withUpdatedLastMessageAt(Instant at) {
        return Conversation.builder()
                .id(id).userId(userId).title(title)
                .status(status).lastMessageAt(at)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}