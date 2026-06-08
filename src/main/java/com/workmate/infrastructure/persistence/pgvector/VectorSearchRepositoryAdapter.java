package com.workmate.infrastructure.persistence.pgvector;

import com.workmate.domain.knowledge.ChunkId;
import com.workmate.domain.knowledge.ChunkIndex;
import com.workmate.domain.knowledge.ChunkMatch;
import com.workmate.domain.knowledge.DocumentChunk;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.Embedding;
import com.workmate.domain.knowledge.SimilarityScore;
import com.workmate.domain.knowledge.VectorSearchRepository;
import com.workmate.domain.workspace.WorkspaceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * {@link VectorSearchRepository} adapter backed by PostgreSQL + pgvector.
 *
 * <p>Chunks are stored in the {@code document_chunks} table (DDL: {@code db/init/03-vector.sql}).
 * Embeddings are bound as pgvector text literals {@code "[f1,f2,…]"} with a {@code ::vector}
 * cast in the SQL to ensure the pgvector driver handles the type regardless of JDBC driver version.
 *
 * <p>Similarity search uses the cosine-distance operator ({@code <=>}); the returned score is
 * {@code 1 - cosine_distance}, clamped to [0.0, 1.0] via {@link SimilarityScore#of(double)}.
 */
@Repository
public class VectorSearchRepositoryAdapter implements VectorSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchRepositoryAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    public VectorSearchRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persists the embeddings for all chunks of a document.
     *
     * <p>Each chunk is inserted with its embedding as a pgvector literal. If a chunk has
     * no embedding attached the embedding column is stored as NULL.
     *
     * @param workspaceId the owning workspace (multi-tenancy scope)
     * @param documentId  the document whose chunks are being saved
     * @param chunks      chunks carrying their computed embeddings; must not be null
     */
    @Override
    public void saveChunks(WorkspaceId workspaceId, DocumentId documentId, List<DocumentChunk> chunks) {
        log.debug("saveChunks: workspaceId={}, documentId={}, count={}", workspaceId, documentId, chunks.size());

        String sql = """
                INSERT INTO document_chunks (id, document_id, workspace_id, chunk_index, content, embedding)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?::vector)
                ON CONFLICT (id) DO UPDATE
                    SET chunk_index = EXCLUDED.chunk_index,
                        content     = EXCLUDED.content,
                        embedding   = EXCLUDED.embedding
                """;

        for (DocumentChunk chunk : chunks) {
            UUID chunkId = chunk.id() != null ? chunk.id().value() : UUID.randomUUID();
            String embeddingLiteral = chunk.embedding() != null
                    ? toVectorLiteral(chunk.embedding().vector())
                    : null;

            jdbcTemplate.update(sql,
                    chunkId.toString(),
                    documentId.value().toString(),
                    workspaceId.value().toString(),
                    chunk.index().value(),
                    chunk.content(),
                    embeddingLiteral);
        }

        log.debug("saveChunks: saved {} chunks for document {}", chunks.size(), documentId);
    }

    /**
     * Returns the {@code topK} chunks most similar to the query embedding within the
     * given workspace, ranked by descending cosine similarity.
     *
     * @param workspaceId the workspace to search within
     * @param query       the query embedding to match against stored chunks
     * @param topK        maximum number of results; must be &gt;= 1
     * @return an unmodifiable list of up to {@code topK} {@link ChunkMatch} hits
     */
    @Override
    public List<ChunkMatch> findSimilar(WorkspaceId workspaceId, Embedding query, int topK) {
        log.debug("findSimilar: workspaceId={}, topK={}", workspaceId, topK);

        String queryLiteral = toVectorLiteral(query.vector());

        String sql = """
                SELECT document_id, id, chunk_index, content,
                       1 - (embedding <=> ?::vector) AS score
                FROM document_chunks
                WHERE workspace_id = ?::uuid
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        List<ChunkMatch> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    UUID documentId = UUID.fromString(rs.getString("document_id"));
                    UUID chunkId    = UUID.fromString(rs.getString("id"));
                    int  chunkIndex = rs.getInt("chunk_index");
                    String content  = rs.getString("content");
                    double score    = rs.getDouble("score");

                    return new ChunkMatch(
                            DocumentId.of(documentId),
                            ChunkId.of(chunkId),
                            chunkIndex,
                            content,
                            SimilarityScore.of(score));
                },
                queryLiteral,
                workspaceId.value().toString(),
                queryLiteral,
                topK);

        log.debug("findSimilar: returned {} results", results.size());
        return List.copyOf(results);
    }

    /**
     * Deletes all stored chunk embeddings for the given document.
     *
     * @param documentId the document whose chunks should be removed
     */
    @Override
    public void deleteByDocument(DocumentId documentId) {
        log.debug("deleteByDocument: documentId={}", documentId);

        int deleted = jdbcTemplate.update(
                "DELETE FROM document_chunks WHERE document_id = ?::uuid",
                documentId.value().toString());

        log.debug("deleteByDocument: deleted {} rows for document {}", deleted, documentId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Formats a float array as a pgvector text literal: {@code "[v1,v2,v3,...]"}.
     *
     * <p>Using the text-literal + {@code ::vector} cast approach is more robust than
     * binding a {@code PGvector} object directly, as it works across all PostgreSQL JDBC
     * driver versions without requiring explicit type registration.
     *
     * @param vector the float array to format
     * @return the pgvector literal string
     */
    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
