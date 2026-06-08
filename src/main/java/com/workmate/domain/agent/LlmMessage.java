package com.workmate.domain.agent;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * A single message in the conversation transcript sent to the LLM.
 *
 * <p>Role is kept as a plain string (e.g. {@code "USER"}, {@code "ASSISTANT"}, {@code "TOOL"})
 * so the agent context stays decoupled from any specific conversation model; the agent loop
 * maps domain messages into this shape.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param role    the speaker role
 * @param content the message text
 */
public record LlmMessage(String role, String content) implements ValueObject {

    public LlmMessage {
        if (role == null || role.isBlank()) {
            throw new DomainException("LlmMessage role must not be blank");
        }
        if (content == null) {
            throw new DomainException("LlmMessage content must not be null");
        }
        role = role.trim();
    }
}
