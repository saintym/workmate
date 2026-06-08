package com.workmate.domain.conversation;

import com.workmate.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Optional;

/**
 * Port (outbound) for persisting and retrieving {@link Conversation} aggregates.
 *
 * <p>Implementations live in the infrastructure layer; this interface belongs to the domain
 * and carries ZERO framework dependencies.
 */
public interface ConversationRepository {

    /**
     * Find a conversation by its identifier.
     *
     * @param id the conversation identifier
     * @return the conversation, or {@link Optional#empty()} if not found
     */
    Optional<Conversation> findById(ConversationId id);

    /**
     * Persist a conversation (insert or update).
     *
     * @param conversation the aggregate to save
     * @return the saved aggregate (may be the same instance or a refreshed copy)
     */
    Conversation save(Conversation conversation);

    /**
     * Find all conversations belonging to a workspace.
     *
     * @param workspaceId the workspace identifier
     * @return all conversations in that workspace, possibly empty
     */
    List<Conversation> findByWorkspace(WorkspaceId workspaceId);
}
