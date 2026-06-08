package com.workmate.domain.workspace;

import com.workmate.domain.common.AggregateRoot;
import com.workmate.domain.workspace.event.WorkspaceCreatedEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root for a tenant workspace — the top-level multi-tenancy boundary.
 *
 * <p>All operations that change workspace state go through this class; external callers
 * must not mutate its collaborators directly. Domain events are accumulated via
 * {@link #registerEvent} and drained by the application layer after a successful save.
 *
 * <p>Pure Java — no framework dependencies.
 */
public final class Workspace extends AggregateRoot<WorkspaceId> {

    private WorkspaceName name;
    private WorkspaceSettings settings;
    private final Instant createdAt;

    /** Internal all-args constructor — callers must use {@link #create} or {@link #reconstitute}. */
    private Workspace(WorkspaceId id, WorkspaceName name, WorkspaceSettings settings, Instant createdAt) {
        super(id);
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Create a brand-new workspace. Generates a fresh {@link WorkspaceId}, stamps
     * {@code createdAt} to now, and registers a {@link WorkspaceCreatedEvent}.
     *
     * @param name the display name for the new workspace; must not be {@code null}
     * @return a new, pending-event-bearing {@code Workspace} instance
     */
    public static Workspace create(WorkspaceName name) {
        Objects.requireNonNull(name, "name must not be null");
        WorkspaceId id = WorkspaceId.newId();
        Workspace workspace = new Workspace(id, name, WorkspaceSettings.empty(), Instant.now());
        workspace.registerEvent(new WorkspaceCreatedEvent(id, name));
        return workspace;
    }

    /**
     * Reconstitute a {@code Workspace} from persisted state (no domain event raised).
     * Intended for use by the persistence mapper only.
     *
     * @param id        the persisted identifier
     * @param name      the persisted name
     * @param settings  the persisted settings
     * @param createdAt the original creation timestamp
     * @return a fully hydrated {@code Workspace} with an empty event buffer
     */
    public static Workspace reconstitute(WorkspaceId id,
                                         WorkspaceName name,
                                         WorkspaceSettings settings,
                                         Instant createdAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        return new Workspace(id, name, settings, createdAt);
    }

    /**
     * Rename the workspace.
     *
     * @param newName the replacement name; must not be {@code null}
     */
    public void rename(WorkspaceName newName) {
        this.name = Objects.requireNonNull(newName, "newName must not be null");
    }

    /**
     * Replace the workspace settings.
     *
     * @param newSettings the replacement settings; must not be {@code null}
     */
    public void updateSettings(WorkspaceSettings newSettings) {
        this.settings = Objects.requireNonNull(newSettings, "newSettings must not be null");
    }

    /** @return the current display name */
    public WorkspaceName name() {
        return name;
    }

    /** @return the current configuration settings */
    public WorkspaceSettings settings() {
        return settings;
    }

    /** @return the instant this workspace was originally created */
    public Instant createdAt() {
        return createdAt;
    }
}
