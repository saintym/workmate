package com.workmate.application.port;

import java.util.UUID;

/**
 * Outbound port for storing raw document files.
 *
 * <p>The application layer depends only on this interface. The infrastructure layer
 * provides the adapter (local file stub for the demo; real S3 in production).
 */
public interface DocumentStorage {

    /**
     * Store document content and return a deterministic storage key.
     *
     * @param workspaceId the owning workspace
     * @param name        the original file name
     * @param contentType the MIME type of the file
     * @param content     the raw text content to persist
     * @return an opaque storage key that can be used to retrieve the content later
     */
    String store(UUID workspaceId, String name, String contentType, String content);

    /**
     * Load document content by its storage key.
     *
     * @param key the storage key returned by {@link #store}
     * @return the raw text content
     * @throws java.io.UncheckedIOException if the content cannot be read
     */
    String load(String key);
}
