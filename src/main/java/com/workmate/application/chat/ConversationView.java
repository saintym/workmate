package com.workmate.application.chat;

import java.util.List;
import java.util.UUID;

/**
 * Read model for a conversation, including its full message history.
 *
 * @param conversationId the unique identifier of the conversation
 * @param workspaceId    the workspace that owns this conversation
 * @param status         the lifecycle status name (e.g. "ACTIVE", "ARCHIVED")
 * @param messages       all messages in the conversation, ordered by sequence
 */
public record ConversationView(UUID conversationId, UUID workspaceId, String status,
                               List<MessageView> messages) {}
