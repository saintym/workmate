package com.workmate.domain.common;

/**
 * Base type for domain rule / invariant violations.
 *
 * <p>Thrown by aggregates and value objects when an operation would break a business
 * invariant. The interface layer maps these to client errors (e.g. HTTP 400/409).
 * Pure Java — no framework dependencies.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
