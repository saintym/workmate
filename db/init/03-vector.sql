-- =============================================================================
-- 03-vector.sql  –  pgvector document_chunks table for RAG Phase 3
-- Runs after 02-business.sql on first Postgres container init.
-- To apply manually to an already-running container use:  db/apply-vector.sh
--
-- Prerequisites: the pgvector extension must already be enabled.
-- 01-init.sql runs "CREATE EXTENSION IF NOT EXISTS vector;" before this file.
-- =============================================================================

CREATE TABLE IF NOT EXISTS document_chunks (
    id           UUID        PRIMARY KEY,
    document_id  UUID        NOT NULL,
    workspace_id UUID        NOT NULL,
    chunk_index  INT         NOT NULL,
    content      TEXT        NOT NULL,
    embedding    vector(1536)
);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_ws
    ON document_chunks(workspace_id);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_doc
    ON document_chunks(document_id);

-- ivfflat approximate nearest-neighbour index using cosine distance.
-- NOTE: ivfflat requires at least (lists * 39) rows to be useful; for a fresh
-- demo database this is fine as an exact scan is used automatically when the
-- table is small. Run ANALYZE after loading data to let the planner choose well.
CREATE INDEX IF NOT EXISTS idx_doc_chunks_vec
    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
