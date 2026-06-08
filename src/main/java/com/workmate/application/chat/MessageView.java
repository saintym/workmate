package com.workmate.application.chat;

import java.time.Instant;

/**
 * Read model for a single message within a conversation.
 *
 * @param role      the author role (e.g. "USER", "ASSISTANT")
 * @param content   the text body of the message
 * @param sequence  the zero-based position of this message in its conversation
 * @param createdAt when the message was created
 */
public record MessageView(String role, String content, int sequence, Instant createdAt) {}
