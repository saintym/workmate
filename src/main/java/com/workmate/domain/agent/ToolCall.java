package com.workmate.domain.agent;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * A request, produced by the LLM, to invoke a specific {@link Tool} with given input.
 *
 * <p>Parsed out of the model's response by the {@link LlmClient} adapter and handed to the
 * agent loop, which resolves the tool by {@code toolName} and executes it with
 * {@code inputJson}.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param toolName  the name of the tool the model wants to call
 * @param inputJson JSON-encoded input for the tool; defaults to {@code "{}"} when blank
 */
public record ToolCall(String toolName, String inputJson) implements ValueObject {

    public ToolCall {
        if (toolName == null || toolName.isBlank()) {
            throw new DomainException("ToolCall toolName must not be blank");
        }
        toolName = toolName.trim();
        if (inputJson == null || inputJson.isBlank()) {
            inputJson = "{}";
        }
    }
}
