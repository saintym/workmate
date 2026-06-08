package com.workmate.domain.knowledge;

import com.workmate.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Optional;

/**
 * Port (outbound) for persisting and retrieving {@link Document} aggregates.
 *
 * <p>The infrastructure layer provides the adapter (e.g. JPA, JDBC). Pure Java — no
 * framework dependencies in this interface.
 */
public interface DocumentRepository {

    /**
     * Find a document by its identifier.
     *
     * @param id the document identifier to look up
     * @return the document, or {@link Optional#empty()} if not found
     */
    Optional<Document> findById(DocumentId id);

    /**
     * Persist a new or updated document (insert-or-update semantics).
     *
     * @param document the aggregate to save
     * @return the saved document (may be the same instance or a refreshed copy)
     */
    Document save(Document document);

    /**
     * Retrieve all documents belonging to the given workspace.
     *
     * @param workspaceId the workspace to query
     * @return a list of documents; empty if none exist
     */
    List<Document> findByWorkspace(WorkspaceId workspaceId);
}
