package com.tripdesigner.conversation.domain;

import java.util.List;
import java.util.Optional;

/**
 * 对话仓储接口。
 */
public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(Long id);
    List<Conversation> findByUserId(Long userId);
    void deleteById(Long id);
}