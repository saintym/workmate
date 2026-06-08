package com.workmate.domain.agent;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;
import com.workmate.domain.workspace.WorkspaceId;

/**
 * Input passed to a {@link Tool} on execution: the LLM-provided arguments plus the
 * <b>tenant context</b> the tool must honour.
 *
 * <p>The {@code workspaceId} is supplied by the agent loop (from the owning conversation),
 * never by the LLM — so tenant-scoped tools (e.g. document search) can isolate data without
 * trusting model output. The {@code json} carries the tool-specific arguments.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param workspaceId the tenant the call is executing for
 * @param json        JSON-encoded tool arguments; defaults to {@code "{}"} when blank
 */
public record ToolInput(WorkspaceId workspaceId, String json) implements ValueObject {

    public ToolInput {
        if (workspaceId == null) {
            throw new DomainException("ToolInput workspaceId must not be null");
        }
        if (json == null || json.isBlank()) {
            json = "{}";
        }
    }
}
