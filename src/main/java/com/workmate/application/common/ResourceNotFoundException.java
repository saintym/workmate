package com.workmate.application.common;

/**
 * Thrown by application services when a requested resource does not exist.
 *
 * <p>The interface layer maps this to a client-visible 404 response.
 * It is distinct from {@link com.workmate.domain.common.DomainException} because absence
 * is an application-level concern, not a domain invariant violation.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
