package com.workmate.domain.knowledge;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * Parameters governing how a document is split into overlapping text windows.
 *
 * <p>Sizes are measured in <em>characters</em>, which is a pragmatic approximation of
 * token windows for this toy project. Token-aware splitting (e.g. tiktoken-based) is a
 * future refinement tracked separately.
 *
 * <p>Consecutive chunks produced by {@link ChunkingService} overlap by {@code overlap}
 * characters, giving the retriever context around chunk boundaries.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code chunkSize >= 1}</li>
 *   <li>{@code overlap >= 0}</li>
 *   <li>{@code overlap < chunkSize} (otherwise the sliding window never advances)</li>
 * </ul>
 */
public record ChunkingStrategy(int chunkSize, int overlap) implements ValueObject {

    public ChunkingStrategy {
        if (chunkSize < 1) {
            throw new DomainException("chunkSize must be >= 1, got: " + chunkSize);
        }
        if (overlap < 0) {
            throw new DomainException("overlap must be >= 0, got: " + overlap);
        }
        if (overlap >= chunkSize) {
            throw new DomainException(
                    "overlap must be < chunkSize, got overlap=" + overlap + ", chunkSize=" + chunkSize);
        }
    }

    /**
     * Returns the default strategy: 500-character windows with a 50-character overlap.
     *
     * @return a {@code ChunkingStrategy} suitable for general-purpose RAG indexing
     */
    public static ChunkingStrategy defaultStrategy() {
        return new ChunkingStrategy(500, 50);
    }
}
