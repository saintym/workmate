package com.workmate.domain.agent;

/**
 * Mutable per-run state of the agent loop — currently the LLM iteration counter, bounded by
 * a {@link MaxIterationsPolicy}.
 *
 * <p>One {@code AgentState} is created per {@link AgentLoop#run} invocation. Each LLM call
 * advances the counter via {@link #beginIteration()}, which enforces the cap.
 *
 * <p>Pure Java — no framework dependencies.
 */
public final class AgentState {

    private final MaxIterationsPolicy policy;
    private int iterations;

    private AgentState(MaxIterationsPolicy policy) {
        this.policy = policy;
        this.iterations = 0;
    }

    /** Start fresh state for one agent-loop run. */
    public static AgentState start(MaxIterationsPolicy policy) {
        return new AgentState(policy);
    }

    /**
     * Advance to the next LLM iteration, enforcing the iteration cap.
     *
     * @return the new (1-based) iteration number
     * @throws MaxIterationsExceededException if the cap has been reached
     */
    public int beginIteration() {
        int next = iterations + 1;
        if (policy.isExceeded(next)) {
            throw new MaxIterationsExceededException(policy.max());
        }
        iterations = next;
        return iterations;
    }

    /** @return how many LLM iterations have run so far. */
    public int iterations() {
        return iterations;
    }
}
