-- pgvector schema for the OLLAMA embedding provider (nomic-embed-text = 768 dims).
-- Use this INSTEAD of 03-vector.sql when EMBEDDING_PROVIDER=ollama.
-- The embedding column dimension MUST match the active EmbeddingService.dimension().
DROP TABLE IF EXISTS document_chunks CASCADE;

CREATE TABLE document_chunks (
    id            uuid PRIMARY KEY,
    document_id   uuid NOT NULL,
    workspace_id  uuid NOT NULL,
    chunk_index   int  NOT NULL,
    content       text NOT NULL,
    embedding     vector(768)
);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_ws  ON document_chunks(workspace_id);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_doc ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_vec
    ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
