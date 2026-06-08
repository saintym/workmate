package com.workmate.domain.knowledge;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * Zero-based positional index of a {@link DocumentChunk} within its parent {@link Document}.
 */
public record ChunkIndex(int value) implements ValueObject {

    public ChunkIndex {
        if (value < 0) {
            throw new DomainException("ChunkIndex must be >= 0, got: " + value);
        }
    }

    public static ChunkIndex of(int value) {
        return new ChunkIndex(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
