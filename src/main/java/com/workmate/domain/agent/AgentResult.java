package com.workmate.domain.agent;

import com.workmate.domain.common.ValueObject;

/**
 * The outcome of an {@link AgentLoop} run: the final assistant answer and how many LLM
 * iterations it took (useful for metrics — tool-call counts, loop depth).
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param finalText  the assistant's final text answer
 * @param iterations the number of LLM iterations performed
 */
public record AgentResult(String finalText, int iterations) implements ValueObject {
}
