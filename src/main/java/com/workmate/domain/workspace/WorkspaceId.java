package com.workmate.domain.workspace;

import com.workmate.domain.common.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link Workspace}. Doubles as the multi-tenancy key — every
 * query in the platform is scoped by a {@code WorkspaceId}.
 */
public record WorkspaceId(UUID value) implements Identifier<UUID> {

    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value must not be null");
    }

    public static WorkspaceId newId() {
        return new WorkspaceId(UUID.randomUUID());
    }

    public static WorkspaceId of(UUID value) {
        return new WorkspaceId(value);
    }

    public static WorkspaceId of(String value) {
        return new WorkspaceId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
