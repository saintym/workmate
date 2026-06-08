package com.workmate.domain.knowledge;

import com.workmate.domain.common.Entity;

import java.util.Objects;

/**
 * A single text chunk of a {@link Document}, optionally carrying its vector {@link Embedding}.
 *
 * <p>Chunks are created without an embedding (embedding is {@code null}) and transition to
 * embedded once the indexing pipeline calls {@link #attachEmbedding(Embedding)}.
 */
public class DocumentChunk extends Entity<ChunkId> {

    private final ChunkIndex index;
    private final String content;
    private Embedding embedding;

    private DocumentChunk(ChunkId id, ChunkIndex index, String content, Embedding embedding) {
        super(id);
        this.index = Objects.requireNonNull(index, "chunk index must not be null");
        if (content == null || content.isBlank()) {
            throw new com.workmate.domain.common.DomainException("Chunk content must not be blank");
        }
        this.content = content;
        this.embedding = embedding;
    }

    /**
     * Creates a new chunk without an embedding (pre-indexing state).
     *
     * @param index   zero-based position within the parent document
     * @param content raw text content of this chunk
     * @return a new {@code DocumentChunk} with a generated id and no embedding
     */
    public static DocumentChunk create(ChunkIndex index, String content) {
        return new DocumentChunk(ChunkId.newId(), index, content, null);
    }

    /**
     * Reconstitutes a fully-hydrated chunk from persistence (no invariant side-effects).
     *
     * @param id        persisted chunk identifier
     * @param index     zero-based position within the parent document
     * @param content   raw text content of this chunk
     * @param embedding the stored vector, or {@code null} if not yet embedded
     * @return a reconstituted {@code DocumentChunk}
     */
    public static DocumentChunk reconstitute(ChunkId id, ChunkIndex index, String content, Embedding embedding) {
        return new DocumentChunk(id, index, content, embedding);
    }

    /**
     * Attaches (or replaces) the vector embedding produced by the indexing pipeline.
     *
     * @param embedding the computed embedding; must not be null
     */
    public void attachEmbedding(Embedding embedding) {
        this.embedding = Objects.requireNonNull(embedding, "embedding must not be null");
    }

    public ChunkIndex index() {
        return index;
    }

    public String content() {
        return content;
    }

    /** @return the vector embedding, or {@code null} if this chunk has not been indexed yet */
    public Embedding embedding() {
        return embedding;
    }
}
