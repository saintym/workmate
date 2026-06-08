package com.workmate.domain.agent;

import java.util.List;
import java.util.Optional;

/**
 * Port giving the agent loop access to the set of available {@link Tool}s.
 *
 * <p>Implemented in the infrastructure layer (it aggregates the concrete tool beans). The
 * domain only needs to look a tool up by name and advertise all definitions to the LLM.
 */
public interface ToolRegistry {

    /**
     * @param name the tool name to resolve
     * @return the matching tool, or empty if none is registered under that name
     */
    Optional<Tool> find(String name);

    /**
     * @return all registered tools
     */
    List<Tool> all();

    /**
     * @return the advertised definitions of all registered tools (sent to the LLM)
     */
    default List<ToolDefinition> definitions() {
        return all().stream().map(Tool::definition).toList();
    }
}
