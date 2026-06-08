package com.workmate.application.chat;

import java.util.List;
import java.util.UUID;

/**
 * Result returned after a {@link SendMessageCommand} is handled.
 *
 * <p>Contains the (possibly newly created) conversation identifier and the full ordered
 * list of messages in the conversation after the user turn and stub assistant reply have
 * been appended.
 *
 * @param conversationId the conversation that was updated or created
 * @param messages       all messages in the conversation, ordered by sequence
 */
public record SendMessageResult(UUID conversationId, List<MessageView> messages) {}
