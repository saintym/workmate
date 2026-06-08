package com.workmate.domain.agent;

/**
 * Raised when the LLM requests a tool that is not registered in the {@link ToolRegistry}.
 */
public class ToolNotFoundException extends AgentException {

    public ToolNotFoundException(String toolName) {
        super("No tool registered under name: " + toolName);
    }
}
