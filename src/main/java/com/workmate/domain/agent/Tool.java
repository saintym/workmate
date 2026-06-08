package com.workmate.domain.agent;

/**
 * A capability that an AI agent can invoke (e.g. web-search, code-execution, SQL query).
 *
 * <p><b>Phase 2 placeholder</b> — tool registration, permission checks, and schema
 * validation are deferred to Phase 2.
 *
 * <p>Pure Java — no framework dependencies.
 */
public interface Tool {

    /**
     * @return the unique name used to identify this tool in agent prompts
     */
    String name();

    /**
     * Execute the tool with the given input.
     *
     * @param inputJson a JSON-encoded string containing the tool's input parameters
     * @return the result of execution; never {@code null}
     */
    ToolResult execute(String inputJson);
}
