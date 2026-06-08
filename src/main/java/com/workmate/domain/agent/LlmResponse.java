package com.workmate.domain.agent;

import com.workmate.domain.common.ValueObject;

import java.util.Collections;
import java.util.List;

/**
 * The LLM's reply at one step of the agent loop.
 *
 * <p>It is either a <b>final text answer</b> (no tool calls) or a <b>request to call one or
 * more tools</b>. The {@link LlmClient} adapter decides which by parsing the provider
 * response; the agent loop only inspects {@link #isToolUse()}.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param text      the assistant text (the final answer when {@code toolCalls} is empty)
 * @param toolCalls the tools the model wants to invoke; empty for a final answer
 */
public record LlmResponse(String text, List<ToolCall> toolCalls) implements ValueObject {

    public LlmResponse {
        text = text == null ? "" : text;
        toolCalls = toolCalls == null ? List.of() : Collections.unmodifiableList(List.copyOf(toolCalls));
    }

    /** @return a final text answer with no tool calls. */
    public static LlmResponse ofText(String text) {
        return new LlmResponse(text, List.of());
    }

    /** @return a tool-use response requesting the given tool calls. */
    public static LlmResponse ofToolCalls(List<ToolCall> toolCalls) {
        return new LlmResponse("", toolCalls);
    }

    /** @return {@code true} if the model requested at least one tool call. */
    public boolean isToolUse() {
        return !toolCalls.isEmpty();
    }
}
