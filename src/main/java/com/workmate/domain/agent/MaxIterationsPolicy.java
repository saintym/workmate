package com.workmate.domain.agent;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * Caps how many times the agent loop may call the LLM before giving up.
 *
 * <p>This is the primary guard against runaway / infinite tool-calling loops (e.g. a tool
 * that keeps returning empty results). Exceeding the cap aborts the loop with a
 * {@link MaxIterationsExceededException}.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param max the maximum number of LLM iterations; must be at least 1
 */
public record MaxIterationsPolicy(int max) implements ValueObject {

    private static final int DEFAULT_MAX = 10;

    public MaxIterationsPolicy {
        if (max < 1) {
            throw new DomainException("MaxIterationsPolicy max must be at least 1");
        }
    }

    /** @return the default policy (10 iterations). */
    public static MaxIterationsPolicy defaultPolicy() {
        return new MaxIterationsPolicy(DEFAULT_MAX);
    }

    /**
     * @param iteration a 1-based iteration count
     * @return {@code true} if the given iteration is beyond the allowed maximum
     */
    public boolean isExceeded(int iteration) {
        return iteration > max;
    }
}
