package com.workmate.domain.conversation;

import com.workmate.domain.common.AggregateRoot;
import com.workmate.domain.common.DomainException;
import com.workmate.domain.conversation.event.MessageAddedEvent;
import com.workmate.domain.workspace.WorkspaceId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a conversation thread between a user and the AI assistant.
 *
 * <p>A {@code Conversation} is always scoped to a single {@link WorkspaceId} and moves
 * through a simple lifecycle: {@link ConversationStatus#ACTIVE} → {@link ConversationStatus#ARCHIVED}.
 * All message mutations go through {@link #addMessage} so invariants are enforced centrally.
 */
public class Conversation extends AggregateRoot<ConversationId> {

    private final WorkspaceId workspaceId;
    private ConversationStatus status;
    private final Instant createdAt;
    private final List<Message> messages;

    private Conversation(ConversationId id, WorkspaceId workspaceId, ConversationStatus status,
                         Instant createdAt, List<Message> messages) {
        super(id);
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.messages = new ArrayList<>(Objects.requireNonNull(messages, "messages must not be null"));
    }

    /**
     * Open a new, empty conversation in the given workspace.
     *
     * @param workspaceId the workspace that owns this conversation
     * @return a new {@code Conversation} in {@link ConversationStatus#ACTIVE} state
     */
    public static Conversation start(WorkspaceId workspaceId) {
        return new Conversation(
                ConversationId.newId(),
                workspaceId,
                ConversationStatus.ACTIVE,
                Instant.now(),
                Collections.emptyList()
        );
    }

    /**
     * Reconstitute a {@code Conversation} from persistent state (no domain events raised).
     *
     * @param id          the persisted identifier
     * @param workspaceId the owning workspace
     * @param status      the persisted status
     * @param createdAt   the original creation timestamp
     * @param messages    the ordered list of messages already in this conversation
     * @return the reconstituted {@code Conversation}
     */
    public static Conversation reconstitute(ConversationId id, WorkspaceId workspaceId,
                                            ConversationStatus status, Instant createdAt,
                                            List<Message> messages) {
        return new Conversation(id, workspaceId, status, createdAt, messages);
    }

    /**
     * Append a new message to this conversation.
     *
     * <p>The sequence number is derived from the current message count so messages are
     * always numbered contiguously from zero.
     *
     * @param role    the author role
     * @param content the message body
     * @return the newly created {@link Message}
     * @throws DomainException if the conversation is not {@link ConversationStatus#ACTIVE}
     */
    public Message addMessage(MessageRole role, MessageContent content) {
        if (status != ConversationStatus.ACTIVE) {
            throw new DomainException(
                    "Cannot add a message to a conversation that is not ACTIVE (current status: " + status + ")");
        }
        int sequence = messages.size();
        Message message = Message.create(role, content, sequence);
        messages.add(message);
        registerEvent(new MessageAddedEvent(id(), workspaceId, message.id(), role));
        return message;
    }

    /**
     * Archive this conversation, making it read-only.
     *
     * <p>Archiving an already-archived conversation is a no-op.
     */
    public void archive() {
        this.status = ConversationStatus.ARCHIVED;
    }

    /** @return the workspace that owns this conversation */
    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    /** @return the current lifecycle status */
    public ConversationStatus status() {
        return status;
    }

    /** @return when this conversation was created */
    public Instant createdAt() {
        return createdAt;
    }

    /** @return an unmodifiable view of the messages in this conversation, ordered by sequence */
    public List<Message> messages() {
        return Collections.unmodifiableList(messages);
    }
}
