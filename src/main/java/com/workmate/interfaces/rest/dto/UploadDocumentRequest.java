package com.workmate.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the POST /api/v1/documents endpoint.
 *
 * @param name        human-readable document name; must not be blank
 * @param contentType MIME type of the file (e.g. "text/plain", "text/markdown")
 * @param sizeBytes   size of the file in bytes
 * @param content     raw text body of the document (markdown or plain text); must not be blank
 */
public record UploadDocumentRequest(@NotBlank String name, String contentType, long sizeBytes,
                                    @NotBlank String content) {}
