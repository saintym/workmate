package com.workmate.application.document;

import java.util.UUID;

/**
 * Result returned after an {@link UploadDocumentCommand} is handled.
 *
 * @param documentId the identifier assigned to the newly created document
 * @param status     the initial lifecycle status name (always "UPLOADED" on success)
 */
public record UploadDocumentResult(UUID documentId, String status) {}
