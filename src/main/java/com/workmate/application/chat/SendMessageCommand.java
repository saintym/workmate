package com.workmate.application.chat;

import java.util.UUID;

/**
 * Command to send a user message in a conversation.
 *
 * <p>If {@code conversationId} is {@code null} a new conversation is started inside the
 * given workspace. If it is non-null the message is appended to the existing conversation.
 *
 * @param workspaceId    the workspace tenant scope; must not be null
 * @param conversationId the target conversation, or null to create a new one
 * @param content        the user-supplied message text; must not be blank
 */
public record SendMessageCommand(UUID workspaceId, UUID conversationId, String content) {}
