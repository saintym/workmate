package com.workmate.domain.knowledge;

import com.workmate.domain.common.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link Document}.
 */
public record DocumentId(UUID value) implements Identifier<UUID> {

    public DocumentId {
        Objects.requireNonNull(value, "DocumentId value must not be null");
    }

    public static DocumentId newId() {
        return new DocumentId(UUID.randomUUID());
    }

    public static DocumentId of(UUID value) {
        return new DocumentId(value);
    }

    public static DocumentId of(String value) {
        return new DocumentId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
