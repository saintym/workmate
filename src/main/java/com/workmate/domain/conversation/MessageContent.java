package com.workmate.domain.conversation;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

import java.util.Objects;

/**
 * The text body of a {@link Message}.
 *
 * <p>Guaranteed to be a non-null, non-blank, trimmed string.
 */
public record MessageContent(String value) implements ValueObject {

    public MessageContent {
        Objects.requireNonNull(value, "MessageContent value must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new DomainException("MessageContent must not be blank");
        }
    }

    public static MessageContent of(String value) {
        return new MessageContent(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
