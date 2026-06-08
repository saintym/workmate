package com.workmate.application.document;

import com.workmate.application.common.ResourceNotFoundException;
import com.workmate.application.port.DocumentStorage;
import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.EventPublisher;
import com.workmate.domain.knowledge.Document;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.DocumentName;
import com.workmate.domain.knowledge.DocumentRepository;
import com.workmate.domain.knowledge.S3Key;
import com.workmate.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service handling document upload CQRS use cases.
 *
 * <p>Coordinates the {@link DocumentRepository}, {@link EventPublisher}, and
 * {@link DocumentStorage} ports. Domain logic (status transitions, event raising) lives
 * entirely inside the {@link Document} aggregate.
 *
 * <p>The {@link DocumentStorage} port is called to obtain the storage key before the
 * aggregate is created, so the key is always persisted alongside the document record.
 * Actual byte transfer is the responsibility of the infrastructure adapter.
 *
 * <p>Phase 3 note: actual S3 upload + Kafka indexing are wired via the
 * {@link com.workmate.domain.knowledge.event.DocumentUploadedEvent} that the aggregate
 * raises; no extra orchestration is needed here.
 */
@Service
public class DocumentUploadService {

    private final DocumentRepository documentRepository;
    private final EventPublisher eventPublisher;
    private final DocumentStorage documentStorage;

    public DocumentUploadService(DocumentRepository documentRepository,
                                 EventPublisher eventPublisher,
                                 DocumentStorage documentStorage) {
        this.documentRepository = Objects.requireNonNull(documentRepository,
                "documentRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher,
                "eventPublisher must not be null");
        this.documentStorage = Objects.requireNonNull(documentStorage,
                "documentStorage must not be null");
    }

    /**
     * Handle an {@link UploadDocumentCommand}.
     *
     * <p>Obtains a storage key from the {@link DocumentStorage} port, creates the
     * {@link Document} aggregate, persists it, and publishes domain events.
     *
     * @param cmd the upload command; must not be null
     * @return the result containing the new document id and its initial status
     */
    @Transactional
    public UploadDocumentResult handle(UploadDocumentCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        String key = documentStorage.store(cmd.workspaceId(), cmd.name(), cmd.contentType());

        Document document = Document.upload(
                WorkspaceId.of(cmd.workspaceId()),
                DocumentName.of(cmd.name()),
                S3Key.of(key));

        document = documentRepository.save(document);
        eventPublisher.publishAll(document.pullDomainEvents());

        // TODO Phase 3: actual S3 upload + Kafka indexing already wired via event.
        return new UploadDocumentResult(document.id().value(), document.status().name());
    }

    /**
     * Handle a {@link GetDocumentQuery}.
     *
     * <p>Loads the document, verifies workspace ownership, and maps to a view.
     *
     * @param query the query; must not be null
     * @return the document view
     * @throws DomainException           if the document belongs to a different workspace
     * @throws ResourceNotFoundException if the document does not exist
     */
    @Transactional(readOnly = true)
    public DocumentView handle(GetDocumentQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Document document = documentRepository
                .findById(DocumentId.of(query.documentId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + query.documentId()));

        guardWorkspaceMatch(document, query.workspaceId());

        return toDocumentView(document);
    }

    // --- private helpers ---

    /**
     * Verify that {@code document} belongs to the given workspace.
     *
     * @throws DomainException if workspace IDs do not match (tenant isolation guard)
     */
    private void guardWorkspaceMatch(Document document, UUID workspaceId) {
        if (!document.workspaceId().value().equals(workspaceId)) {
            throw new DomainException(
                    "Document " + document.id() + " does not belong to workspace " + workspaceId);
        }
    }

    /** Map a {@link Document} to its {@link DocumentView} read model. */
    private DocumentView toDocumentView(Document document) {
        return new DocumentView(
                document.id().value(),
                document.workspaceId().value(),
                document.name().value(),
                document.status().name(),
                document.createdAt());
    }
}
