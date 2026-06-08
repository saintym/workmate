package com.workmate.application.document;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model for a document.
 *
 * @param documentId  the unique identifier of the document
 * @param workspaceId the workspace that owns this document
 * @param name        the human-readable document name
 * @param status      the lifecycle status name (e.g. "UPLOADED", "INDEXING", "INDEXED", "FAILED")
 * @param createdAt   when the document was uploaded
 */
public record DocumentView(UUID documentId, UUID workspaceId, String name, String status,
                           Instant createdAt) {}
