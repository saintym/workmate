package com.workmate.domain.agent;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * The advertised specification of a {@link Tool} — what the LLM is told it can call.
 *
 * <p>The {@code inputSchemaJson} is a JSON-Schema string describing the tool's expected
 * input object; the LLM uses it to produce a valid {@link ToolCall}. How this definition is
 * rendered into the model prompt (and how the model's reply is parsed back) is the concern
 * of the {@link LlmClient} adapter, not the domain.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param name            unique tool name (referenced in tool calls)
 * @param description     natural-language description of what the tool does and when to use it
 * @param inputSchemaJson JSON-Schema for the tool input; defaults to {@code "{}"} when blank
 */
public record ToolDefinition(String name, String description, String inputSchemaJson)
        implements ValueObject {

    public ToolDefinition {
        if (name == null || name.isBlank()) {
            throw new DomainException("ToolDefinition name must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new DomainException("ToolDefinition description must not be blank");
        }
        name = name.trim();
        if (inputSchemaJson == null || inputSchemaJson.isBlank()) {
            inputSchemaJson = "{}";
        }
    }
}
