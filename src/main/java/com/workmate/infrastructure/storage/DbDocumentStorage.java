package com.workmate.infrastructure.storage;

import com.workmate.application.port.DocumentStorage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Database-backed {@link DocumentStorage}: keeps document text in PostgreSQL.
 *
 * <p>Unlike {@link LocalDocumentStorage}, this works across pods — the API node writes the
 * content and the indexing worker reads it back through the shared database. It is the demo's
 * stand-in for a shared object store (S3/MinIO); good enough for text documents, though a real
 * deployment would stream large binaries to object storage instead.
 *
 * <p>Activated with {@code app.storage.provider=db}.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "db")
public class DbDocumentStorage implements DocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(DbDocumentStorage.class);

    private final JdbcTemplate jdbcTemplate;

    public DbDocumentStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Ensure the backing table exists (kept self-contained so no extra migration is needed). */
    @PostConstruct
    void initSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS document_contents (
                storage_key text PRIMARY KEY,
                content     text NOT NULL,
                created_at  timestamptz NOT NULL DEFAULT now()
            )
            """);
        log.info("DbDocumentStorage ready (table document_contents ensured)");
    }

    @Override
    public String store(UUID workspaceId, String name, String contentType, String content) {
        String key = "workspaces/" + workspaceId + "/documents/" + UUID.randomUUID() + "/" + name;
        jdbcTemplate.update("""
            INSERT INTO document_contents(storage_key, content) VALUES (?, ?)
            ON CONFLICT (storage_key) DO UPDATE SET content = EXCLUDED.content
            """, key, content == null ? "" : content);
        log.info("Stored document in DB: workspaceId={}, name={}, key={}", workspaceId, name, key);
        return key;
    }

    @Override
    public String load(String key) {
        try {
            String content = jdbcTemplate.queryForObject(
                    "SELECT content FROM document_contents WHERE storage_key = ?", String.class, key);
            log.debug("Loaded document from DB: key={}", key);
            return content == null ? "" : content;
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("No document content stored at key: " + key, e);
        }
    }
}
