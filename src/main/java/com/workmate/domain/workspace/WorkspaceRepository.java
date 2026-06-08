package com.workmate.domain.workspace;

import java.util.Optional;

/**
 * Port interface for {@link Workspace} persistence.
 *
 * <p>Implementations live in the infrastructure layer (e.g. a JPA adapter). The domain
 * layer depends only on this interface and therefore remains free of persistence concerns.
 *
 * <p>Pure Java — no framework dependencies.
 */
public interface WorkspaceRepository {

    /**
     * Look up a workspace by its identifier.
     *
     * @param id the workspace's typed identifier; must not be {@code null}
     * @return an {@link Optional} containing the workspace if found, or empty
     */
    Optional<Workspace> findById(WorkspaceId id);

    /**
     * Persist a new or updated workspace and return the saved instance.
     *
     * @param workspace the aggregate to save; must not be {@code null}
     * @return the saved (possibly re-hydrated) workspace
     */
    Workspace save(Workspace workspace);

    /**
     * Check whether a workspace with the given identifier already exists.
     *
     * @param id the workspace's typed identifier; must not be {@code null}
     * @return {@code true} if a workspace with this id exists, {@code false} otherwise
     */
    boolean existsById(WorkspaceId id);
}
