package com.workmate.domain.workspace.event;

import com.workmate.domain.common.AbstractDomainEvent;
import com.workmate.domain.workspace.WorkspaceId;
import com.workmate.domain.workspace.WorkspaceName;

/**
 * Raised when a new tenant {@code Workspace} is created.
 */
public final class WorkspaceCreatedEvent extends AbstractDomainEvent {

    private final WorkspaceId workspaceId;
    private final WorkspaceName name;

    public WorkspaceCreatedEvent(WorkspaceId workspaceId, WorkspaceName name) {
        this.workspaceId = workspaceId;
        this.name = name;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public WorkspaceName name() {
        return name;
    }
}
