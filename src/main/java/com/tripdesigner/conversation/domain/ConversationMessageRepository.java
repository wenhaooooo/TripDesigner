package com.tripdesigner.conversation.domain;

import java.util.List;
import java.util.Optional;

/**
 * 对话消息仓储接口。
 */
public interface ConversationMessageRepository {
    ConversationMessage save(ConversationMessage message);
    Optional<ConversationMessage> findById(Long id);
    List<ConversationMessage> findByConversationId(Long conversationId);
    void deleteByConversationId(Long conversationId);
}