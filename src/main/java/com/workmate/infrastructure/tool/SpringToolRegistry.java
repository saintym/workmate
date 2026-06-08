package com.workmate.infrastructure.tool;

import com.workmate.domain.agent.Tool;
import com.workmate.domain.agent.ToolRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring-backed {@link ToolRegistry} that aggregates all {@link Tool} beans registered in the
 * application context.
 *
 * <p>Spring injects every {@code Tool} implementation (e.g. {@code QueryDatabaseTool}) via the
 * constructor list. If no tools are registered yet the registry starts empty — that is fine.
 */
@Component
public class SpringToolRegistry implements ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(SpringToolRegistry.class);

    private final Map<String, Tool> toolsByName;
    private final List<Tool> tools;

    public SpringToolRegistry(List<Tool> tools) {
        this.tools = Collections.unmodifiableList(tools);
        this.toolsByName = tools.stream()
                .collect(Collectors.toMap(Tool::name, Function.identity()));
    }

    @PostConstruct
    void logRegisteredTools() {
        if (toolsByName.isEmpty()) {
            log.info("ToolRegistry initialised with no tools registered.");
        } else {
            log.info("ToolRegistry initialised with {} tool(s): {}", toolsByName.size(),
                    toolsByName.keySet());
        }
    }

    @Override
    public Optional<Tool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    @Override
    public List<Tool> all() {
        return tools;
    }
}
