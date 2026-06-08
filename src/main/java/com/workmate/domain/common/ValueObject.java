package com.workmate.domain.common;

/**
 * Marker interface for Value Objects.
 *
 * <p>A Value Object has no identity; it is defined entirely by its attributes and is
 * immutable. Two value objects are equal when all their attributes are equal, so every
 * implementation must provide proper {@code equals}/{@code hashCode} — Java records are
 * the recommended implementation.
 *
 * <p>Pure Java — the domain layer has ZERO Spring/framework dependencies.
 */
public interface ValueObject {
}
