package com.tripdesigner.conversation.api.vo;
/**
 * 对话消息视图对象（VO）。
 */

import com.tripdesigner.conversation.domain.ConversationMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ConversationMessageVo {
    private final Long id;
    private final String role;
    private final String content;
    private final String metadata;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ConversationMessageVo from(ConversationMessage m) {
        return ConversationMessageVo.builder()
                .id(m.getId())
                .role(m.getRole() != null ? m.getRole().name() : null)
                .content(m.getContent())
                .metadata(m.getMetadata())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
