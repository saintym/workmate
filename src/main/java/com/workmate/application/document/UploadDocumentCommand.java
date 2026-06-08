package com.workmate.application.document;

import java.util.UUID;

/**
 * Command to upload a document into a workspace for RAG indexing.
 *
 * @param workspaceId the workspace that will own the document; must not be null
 * @param name        the human-readable document name; must not be blank
 * @param contentType the MIME type of the file (e.g. "text/plain", "text/markdown")
 * @param sizeBytes   the size of the file in bytes
 * @param content     the raw text body of the document (markdown or plain text)
 */
public record UploadDocumentCommand(UUID workspaceId, String name, String contentType,
                                    long sizeBytes, String content) {}
