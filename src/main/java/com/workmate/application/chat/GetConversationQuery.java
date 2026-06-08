package com.workmate.application.chat;

import java.util.UUID;

/**
 * Query to retrieve a single conversation with its full message history.
 *
 * @param workspaceId    the tenant scope; used to verify ownership
 * @param conversationId the conversation to retrieve
 */
public record GetConversationQuery(UUID workspaceId, UUID conversationId) {}
