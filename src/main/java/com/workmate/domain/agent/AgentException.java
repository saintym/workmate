package com.workmate.domain.agent;

import com.workmate.domain.common.DomainException;

/**
 * Base type for failures raised inside the agent loop.
 */
public class AgentException extends DomainException {

    public AgentException(String message) {
        super(message);
    }
}
