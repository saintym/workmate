package com.workmate.domain.agent;

/**
 * Raised when the agent loop reaches its {@link MaxIterationsPolicy} cap without the LLM
 * producing a final text answer — the safeguard against infinite tool-calling loops.
 */
public class MaxIterationsExceededException extends AgentException {

    public MaxIterationsExceededException(int max) {
        super("Agent loop exceeded the maximum of " + max + " iterations without a final answer");
    }
}
