package com.workmate.domain.knowledge.event;

import com.workmate.domain.common.AbstractDomainEvent;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.DocumentName;
import com.workmate.domain.knowledge.S3Key;
import com.workmate.domain.workspace.WorkspaceId;

/**
 * Raised when a new {@link com.workmate.domain.knowledge.Document} is uploaded to a workspace.
 *
 * <p>The Kafka indexing pipeline (Phase 3) consumes this event to trigger chunking and
 * embedding of the document referenced by {@link #s3Key()}.
 */
public final class DocumentUploadedEvent extends AbstractDomainEvent {

    private final DocumentId documentId;
    private final WorkspaceId workspaceId;
    private final S3Key s3Key;
    private final DocumentName documentName;

    public DocumentUploadedEvent(
            DocumentId documentId,
            WorkspaceId workspaceId,
            S3Key s3Key,
            DocumentName documentName) {
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.s3Key = s3Key;
        this.documentName = documentName;
    }

    public DocumentId documentId() {
        return documentId;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public S3Key s3Key() {
        return s3Key;
    }

    public DocumentName documentName() {
        return documentName;
    }
}
