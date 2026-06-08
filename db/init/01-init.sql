-- Runs once on first PostgreSQL container init.
-- Enables pgvector so document_chunks.embedding (vector(1536)) is usable.
-- Phase 1 platform tables are created by Hibernate at boot; a hand-written
-- migration set (Flyway) and business-data seed (products/orders/refunds) land in Phase 2.
CREATE EXTENSION IF NOT EXISTS vector;
