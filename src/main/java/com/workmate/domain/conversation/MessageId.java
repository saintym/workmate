package com.workmate.domain.conversation;

import com.workmate.domain.common.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link Message} entity.
 */
public record MessageId(UUID value) implements Identifier<UUID> {

    public MessageId {
        Objects.requireNonNull(value, "MessageId value must not be null");
    }

    public static MessageId newId() {
        return new MessageId(UUID.randomUUID());
    }

    public static MessageId of(UUID value) {
        return new MessageId(value);
    }

    public static MessageId of(String value) {
        return new MessageId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
