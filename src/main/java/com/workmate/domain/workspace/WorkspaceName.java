package com.workmate.domain.workspace;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * The display name of a tenant workspace. Trimmed, non-blank, bounded length.
 */
public record WorkspaceName(String value) implements ValueObject {

    private static final int MAX_LENGTH = 120;

    public WorkspaceName {
        if (value == null || value.isBlank()) {
            throw new DomainException("Workspace name must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new DomainException("Workspace name must be at most " + MAX_LENGTH + " characters");
        }
    }

    public static WorkspaceName of(String value) {
        return new WorkspaceName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
