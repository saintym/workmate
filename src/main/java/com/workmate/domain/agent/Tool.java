package com.workmate.domain.agent;

/**
 * A capability an AI agent can invoke during the agent loop (e.g. SQL query, document
 * search, employee lookup).
 *
 * <p>A tool advertises itself via a {@link ToolDefinition} (so the LLM knows it exists and
 * how to call it) and performs work via {@link #execute(String)}. Implementations live in
 * the infrastructure layer; this port is pure Java.
 */
public interface Tool {

    /**
     * @return this tool's advertised specification (name, description, input schema)
     */
    ToolDefinition definition();

    /**
     * @return the unique name used to identify this tool in agent prompts and tool calls
     */
    default String name() {
        return definition().name();
    }

    /**
     * Execute the tool with the given input.
     *
     * @param inputJson a JSON-encoded string containing the tool's input parameters
     * @return the result of execution; never {@code null}
     */
    ToolResult execute(String inputJson);
}
