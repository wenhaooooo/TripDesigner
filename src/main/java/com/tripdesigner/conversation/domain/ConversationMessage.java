package com.tripdesigner.conversation.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 对话消息领域实体。
 * 代表对话中的一条消息，可能是用户发送或 AI 回复。
 * metadata 字段存储 JSON 格式的附加信息（如工作流关联数据）。
 */
@Getter
@Builder
public class ConversationMessage {
    private Long id;
    private Long conversationId;
    private Long userId;
    private ConversationRole role;
    private String content;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static ConversationMessage of(Long conversationId, Long userId, ConversationRole role,
                                         String content, String metadata) {
        return ConversationMessage.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(role)
                .content(content)
                .metadata(metadata)
                .version(0)
                .build();
    }
}