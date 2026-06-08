package com.workmate.domain.conversation;

/**
 * Lifecycle status of a {@link Conversation}.
 */
public enum ConversationStatus {

    /** The conversation is open and accepts new messages. */
    ACTIVE,

    /** The conversation has been closed and is read-only. */
    ARCHIVED
}
