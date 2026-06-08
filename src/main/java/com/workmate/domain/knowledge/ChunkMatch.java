package com.workmate.domain.knowledge;

import com.workmate.domain.common.ValueObject;

import java.util.Objects;

/**
 * A single hit returned by a vector similarity search.
 *
 * <p>Carries enough information for the application layer to assemble a RAG context
 * window: the source document, the chunk's position and raw text, and how closely it
 * matched the query embedding.
 */
public record ChunkMatch(
        DocumentId documentId,
        ChunkId chunkId,
        int chunkIndex,
        String content,
        SimilarityScore score) implements ValueObject {

    public ChunkMatch {
        Objects.requireNonNull(documentId, "documentId must not be null");
        Objects.requireNonNull(chunkId, "chunkId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(score, "score must not be null");
    }
}
