package com.workmate.interfaces.rest;

import com.workmate.application.document.DocumentUploadService;
import com.workmate.application.document.DocumentView;
import com.workmate.application.document.GetDocumentQuery;
import com.workmate.application.document.UploadDocumentCommand;
import com.workmate.application.document.UploadDocumentResult;
import com.workmate.interfaces.rest.dto.UploadDocumentRequest;
import com.workmate.interfaces.rest.dto.UploadDocumentResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for document upload and retrieval operations.
 *
 * <p>POST /api/v1/documents accepts document metadata and returns 202 ACCEPTED while
 * background RAG indexing proceeds asynchronously.
 * GET /api/v1/documents/{id} returns the current document view.
 */
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentUploadService documentUploadService;

    public DocumentController(DocumentUploadService documentUploadService) {
        this.documentUploadService = Objects.requireNonNull(documentUploadService,
                "documentUploadService must not be null");
    }

    /**
     * Upload a document into the workspace for RAG indexing.
     *
     * <p>The blocking {@link DocumentUploadService#handle(UploadDocumentCommand)} call is
     * offloaded to {@code boundedElastic()} so the event-loop thread is never blocked.
     * Returns HTTP 202 ACCEPTED because indexing happens asynchronously.
     *
     * @param workspaceId the tenant workspace UUID from {@code X-Workspace-Id} header
     * @param request     the validated request body
     * @return 202 ACCEPTED with the document ID and initial status
     */
    @PostMapping("/documents")
    public Mono<ResponseEntity<UploadDocumentResponse>> uploadDocument(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @Valid @RequestBody UploadDocumentRequest request) {

        log.debug("uploadDocument workspaceId={} name={}", workspaceId, request.name());

        UploadDocumentCommand cmd = new UploadDocumentCommand(
                workspaceId, request.name(), request.contentType(), request.sizeBytes());

        return Mono.fromCallable(() -> documentUploadService.handle(cmd))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> {
                    UploadDocumentResponse body = new UploadDocumentResponse(
                            result.documentId(), result.status());
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
                });
    }

    /**
     * Retrieve document metadata for a single document.
     *
     * @param workspaceId the tenant workspace UUID from {@code X-Workspace-Id} header
     * @param documentId  the document to retrieve
     * @return the document view
     */
    @GetMapping("/documents/{id}")
    public Mono<DocumentView> getDocument(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable("id") UUID documentId) {

        log.debug("getDocument workspaceId={} documentId={}", workspaceId, documentId);

        GetDocumentQuery query = new GetDocumentQuery(workspaceId, documentId);
        return Mono.fromCallable(() -> documentUploadService.handle(query))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
