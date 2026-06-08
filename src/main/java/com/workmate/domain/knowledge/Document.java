package com.workmate.domain.knowledge;

import com.workmate.domain.common.AggregateRoot;
import com.workmate.domain.common.DomainException;
import com.workmate.domain.knowledge.event.DocumentUploadedEvent;
import com.workmate.domain.workspace.WorkspaceId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a document uploaded to a workspace for RAG indexing.
 *
 * <p>Lifecycle: {@code UPLOADED → INDEXING → INDEXED} (happy path) or
 * {@code UPLOADED | INDEXING → FAILED} (error path). Illegal status transitions throw
 * {@link DomainException}.
 */
public class Document extends AggregateRoot<DocumentId> {

    private final WorkspaceId workspaceId;
    private final DocumentName name;
    private final S3Key s3Key;
    private DocumentStatus status;
    private final Instant createdAt;
    private final List<DocumentChunk> chunks;

    private Document(
            DocumentId id,
            WorkspaceId workspaceId,
            DocumentName name,
            S3Key s3Key,
            DocumentStatus status,
            Instant createdAt,
            List<DocumentChunk> chunks) {
        super(id);
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.s3Key = Objects.requireNonNull(s3Key, "s3Key must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.chunks = new ArrayList<>(Objects.requireNonNull(chunks, "chunks must not be null"));
    }

    /**
     * Creates a new document from an upload event. Registers a {@link DocumentUploadedEvent}
     * to trigger the downstream indexing pipeline.
     *
     * @param workspaceId the owning workspace
     * @param name        the human-readable document name
     * @param s3Key       the S3 object key of the uploaded file
     * @return a new {@code Document} in {@link DocumentStatus#UPLOADED} state
     */
    public static Document upload(WorkspaceId workspaceId, DocumentName name, S3Key s3Key) {
        DocumentId id = DocumentId.newId();
        Document doc = new Document(id, workspaceId, name, s3Key, DocumentStatus.UPLOADED, Instant.now(), List.of());
        doc.registerEvent(new DocumentUploadedEvent(id, workspaceId, s3Key, name));
        return doc;
    }

    /**
     * Reconstitutes a fully-hydrated document from persistence (no domain events registered).
     *
     * @param id          persisted document identifier
     * @param workspaceId the owning workspace
     * @param name        the human-readable document name
     * @param s3Key       the S3 object key of the uploaded file
     * @param status      current lifecycle status
     * @param createdAt   original creation timestamp
     * @param chunks      previously persisted chunks
     * @return a reconstituted {@code Document}
     */
    public static Document reconstitute(
            DocumentId id,
            WorkspaceId workspaceId,
            DocumentName name,
            S3Key s3Key,
            DocumentStatus status,
            Instant createdAt,
            List<DocumentChunk> chunks) {
        return new Document(id, workspaceId, name, s3Key, status, createdAt, chunks);
    }

    /**
     * Transitions this document to {@link DocumentStatus#INDEXING}.
     *
     * @throws DomainException if the current status is not {@link DocumentStatus#UPLOADED}
     */
    public void markIndexing() {
        if (status != DocumentStatus.UPLOADED) {
            throw new DomainException(
                    "Cannot start indexing a document in status " + status + "; expected UPLOADED");
        }
        status = DocumentStatus.INDEXING;
    }

    /**
     * Appends a chunk to this document. Only valid while the document is in
     * {@link DocumentStatus#INDEXING} state.
     *
     * @param chunk the chunk to add; must not be null
     * @throws DomainException if the document is not currently {@link DocumentStatus#INDEXING}
     */
    public void addChunk(DocumentChunk chunk) {
        if (status != DocumentStatus.INDEXING) {
            throw new DomainException(
                    "Cannot add chunks to a document in status " + status + "; expected INDEXING");
        }
        Objects.requireNonNull(chunk, "chunk must not be null");
        chunks.add(chunk);
    }

    /**
     * Transitions this document to {@link DocumentStatus#INDEXED} once all chunks have been
     * embedded and stored.
     *
     * @throws DomainException if the current status is not {@link DocumentStatus#INDEXING}
     */
    public void markIndexed() {
        if (status != DocumentStatus.INDEXING) {
            throw new DomainException(
                    "Cannot mark a document indexed when in status " + status + "; expected INDEXING");
        }
        status = DocumentStatus.INDEXED;
    }

    /**
     * Transitions this document to {@link DocumentStatus#FAILED} after an unrecoverable
     * indexing error. Valid from {@link DocumentStatus#UPLOADED} or {@link DocumentStatus#INDEXING}.
     *
     * @throws DomainException if the document is already {@link DocumentStatus#INDEXED} or
     *                         {@link DocumentStatus#FAILED}
     */
    public void markFailed() {
        if (status == DocumentStatus.INDEXED || status == DocumentStatus.FAILED) {
            throw new DomainException(
                    "Cannot mark a document failed when in status " + status);
        }
        status = DocumentStatus.FAILED;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public DocumentName name() {
        return name;
    }

    public S3Key s3Key() {
        return s3Key;
    }

    public DocumentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** @return an unmodifiable view of this document's chunks */
    public List<DocumentChunk> chunks() {
        return Collections.unmodifiableList(chunks);
    }
}
