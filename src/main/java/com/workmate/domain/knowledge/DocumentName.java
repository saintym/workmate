package com.workmate.domain.knowledge;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * The display name of a {@link Document}. Trimmed, non-blank, bounded length.
 */
public record DocumentName(String value) implements ValueObject {

    private static final int MAX_LENGTH = 255;

    public DocumentName {
        if (value == null || value.isBlank()) {
            throw new DomainException("Document name must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new DomainException("Document name must be at most " + MAX_LENGTH + " characters");
        }
    }

    public static DocumentName of(String value) {
        return new DocumentName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
