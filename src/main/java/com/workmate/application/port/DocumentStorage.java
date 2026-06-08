package com.workmate.application.port;

import java.util.UUID;

/**
 * Outbound port for storing raw document files.
 *
 * <p>The application layer depends only on this interface. The infrastructure layer
 * provides the adapter (local stub for Phase 1; real S3 in Phase 3).
 */
public interface DocumentStorage {

    /**
     * Store a document and return a deterministic storage key.
     *
     * @param workspaceId the owning workspace
     * @param name        the original file name
     * @param contentType the MIME type of the file
     * @return an opaque storage key that can be used to retrieve the object later
     */
    String store(UUID workspaceId, String name, String contentType);
}
