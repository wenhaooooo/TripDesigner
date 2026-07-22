package com.tripdesigner.conversation.api.vo;
/**
 * 对话视图对象（VO）。
 */

import com.tripdesigner.conversation.domain.Conversation;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ConversationVo {
    private final Long id;
    private final String title;
    private final String status;
    private final Instant lastMessageAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ConversationVo from(Conversation c) {
        return ConversationVo.builder()
                .id(c.getId())
                .title(c.getTitle())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
