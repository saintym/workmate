package com.workmate.domain.knowledge;

import com.workmate.domain.common.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link DocumentChunk}.
 */
public record ChunkId(UUID value) implements Identifier<UUID> {

    public ChunkId {
        Objects.requireNonNull(value, "ChunkId value must not be null");
    }

    public static ChunkId newId() {
        return new ChunkId(UUID.randomUUID());
    }

    public static ChunkId of(UUID value) {
        return new ChunkId(value);
    }

    public static ChunkId of(String value) {
        return new ChunkId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
