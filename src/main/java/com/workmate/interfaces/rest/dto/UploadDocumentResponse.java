package com.workmate.interfaces.rest.dto;

import java.util.UUID;

/**
 * Response body for the POST /api/v1/documents endpoint.
 *
 * @param documentId the identifier assigned to the newly created document
 * @param status     the initial lifecycle status (always "UPLOADED" on success)
 */
public record UploadDocumentResponse(UUID documentId, String status) {}
