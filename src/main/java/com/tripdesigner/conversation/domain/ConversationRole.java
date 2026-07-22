package com.tripdesigner.conversation.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConversationRole {
    USER,
    ASSISTANT;

    @JsonCreator
    public static ConversationRole fromString(String value) {
        if (value == null) {
            return null;
        }
        return ConversationRole.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}