package com.workmate.domain.agent;

import com.workmate.domain.common.ValueObject;

/**
 * The outcome of executing a {@link Tool}.
 *
 * <p><b>Phase 2 placeholder</b> — full tool-execution semantics (retries, structured output
 * schema validation, streaming) are deferred to Phase 2.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param success    {@code true} if the tool executed without error
 * @param outputJson the tool's JSON-encoded output or error description
 */
public record ToolResult(boolean success, String outputJson) implements ValueObject {

    /**
     * Create a successful result.
     *
     * @param outputJson the JSON-encoded output; must not be {@code null}
     * @return a successful {@code ToolResult}
     */
    public static ToolResult ok(String outputJson) {
        return new ToolResult(true, outputJson);
    }

    /**
     * Create an error result.
     *
     * @param outputJson a JSON-encoded description of the error; must not be {@code null}
     * @return an error {@code ToolResult}
     */
    public static ToolResult error(String outputJson) {
        return new ToolResult(false, outputJson);
    }
}
