package com.workmate.application.document;

import java.util.UUID;

/**
 * Query to retrieve metadata for a single document.
 *
 * @param workspaceId the tenant scope; used to verify ownership
 * @param documentId  the document to retrieve
 */
public record GetDocumentQuery(UUID workspaceId, UUID documentId) {}
