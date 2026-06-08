package com.workmate.domain.knowledge;

import com.workmate.domain.workspace.WorkspaceId;

import java.util.List;

/**
 * Port interface (driven port) for persisting and querying chunk embeddings.
 *
 * <p>The implementation lives in {@code infrastructure/persistence/pgvector} and stores
 * vectors in a PostgreSQL {@code pgvector} extension column, performing approximate
 * nearest-neighbour search via cosine similarity ({@code <=>} operator).
 *
 * <p>All operations are scoped to a {@link WorkspaceId} to enforce multi-tenancy.
 *
 * <p>Pure Java — no Spring/framework imports.
 */
public interface VectorSearchRepository {

    /**
     * Persists the embeddings for all chunks of a document.
     * Chunks must already have embeddings attached via
     * {@link DocumentChunk#attachEmbedding(Embedding)}.
     *
     * @param workspaceId the owning workspace (multi-tenancy scope)
     * @param documentId  the document whose chunks are being saved
     * @param chunks      chunks carrying their computed embeddings; must not be null
     */
    void saveChunks(WorkspaceId workspaceId, DocumentId documentId, List<DocumentChunk> chunks);

    /**
     * Returns the {@code topK} chunks most similar to the query embedding within the
     * given workspace, ranked by descending cosine similarity.
     *
     * @param workspaceId the workspace to search within
     * @param query       the query embedding to match against stored chunks
     * @param topK        maximum number of results to return; must be >= 1
     * @return an unmodifiable list of up to {@code topK} {@link ChunkMatch} hits,
     *         ordered by descending similarity score
     */
    List<ChunkMatch> findSimilar(WorkspaceId workspaceId, Embedding query, int topK);

    /**
     * Deletes all stored chunk embeddings for the given document.
     *
     * <p>Called before re-indexing a document to ensure idempotency — existing vectors
     * are purged before the new ones are written.
     *
     * @param documentId the document whose chunks should be removed
     */
    void deleteByDocument(DocumentId documentId);
}
