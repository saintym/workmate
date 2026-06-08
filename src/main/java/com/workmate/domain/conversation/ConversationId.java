package com.workmate.domain.conversation;

import com.workmate.domain.common.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link Conversation} aggregate.
 */
public record ConversationId(UUID value) implements Identifier<UUID> {

    public ConversationId {
        Objects.requireNonNull(value, "ConversationId value must not be null");
    }

    public static ConversationId newId() {
        return new ConversationId(UUID.randomUUID());
    }

    public static ConversationId of(UUID value) {
        return new ConversationId(value);
    }

    public static ConversationId of(String value) {
        return new ConversationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
