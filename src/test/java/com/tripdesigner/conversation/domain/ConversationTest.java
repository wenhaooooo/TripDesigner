package com.tripdesigner.conversation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {

    @Test
    void create_sets_active_status_and_default_title() {
        Conversation c = Conversation.create(1L, "My Trip Discussion");
        assertNull(c.getId());
        assertEquals("My Trip Discussion", c.getTitle());
        assertEquals(ConversationStatus.ACTIVE, c.getStatus());
        assertEquals(0, c.getVersion());
    }

    @Test
    void create_with_null_title_uses_default() {
        Conversation c = Conversation.create(1L, null);
        assertEquals("New Conversation", c.getTitle());
    }

    @Test
    void withUpdatedTitle_changes_title() {
        Conversation c = Conversation.create(1L, "Old Title");
        Conversation updated = c.withUpdatedTitle("New Title");
        assertEquals("New Title", updated.getTitle());
        assertEquals(1L, updated.getUserId());
    }

    @Test
    void withUpdatedLastMessageAt_updates_timestamp() {
        Conversation c = Conversation.create(1L, "Title");
        Instant now = Instant.now();
        Conversation updated = c.withUpdatedLastMessageAt(now);
        assertEquals(now, updated.getLastMessageAt());
    }

    @Test
    void conversationMessage_create_sets_version_zero() {
        ConversationMessage m = ConversationMessage.of(1L, ConversationRole.USER, "Hello", null);
        assertNull(m.getId());
        assertEquals(ConversationRole.USER, m.getRole());
        assertEquals("Hello", m.getContent());
        assertEquals(0, m.getVersion());
    }
}
