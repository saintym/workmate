package com.workmate.domain.conversation.event;

import com.workmate.domain.common.AbstractDomainEvent;
import com.workmate.domain.conversation.ConversationId;
import com.workmate.domain.conversation.MessageId;
import com.workmate.domain.conversation.MessageRole;
import com.workmate.domain.workspace.WorkspaceId;

import java.util.Objects;

/**
 * Raised when a {@link com.workmate.domain.conversation.Message} is successfully appended
 * to a {@link com.workmate.domain.conversation.Conversation}.
 */
public final class MessageAddedEvent extends AbstractDomainEvent {

    private final ConversationId conversationId;
    private final WorkspaceId workspaceId;
    private final MessageId messageId;
    private final MessageRole role;

    public MessageAddedEvent(ConversationId conversationId, WorkspaceId workspaceId,
                             MessageId messageId, MessageRole role) {
        super();
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    /** @return the id of the conversation that received the message */
    public ConversationId conversationId() {
        return conversationId;
    }

    /** @return the workspace that owns the conversation */
    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    /** @return the id of the newly added message */
    public MessageId messageId() {
        return messageId;
    }

    /** @return the role of the author of the new message */
    public MessageRole role() {
        return role;
    }
}
