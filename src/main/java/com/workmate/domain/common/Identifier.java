package com.workmate.domain.common;

/**
 * Marker for typed identifiers (e.g. {@code WorkspaceId}, {@code ConversationId}).
 *
 * <p>Typed ids prevent accidentally passing one aggregate's id where another's is
 * expected. Pure Java — no framework dependencies.
 *
 * @param <T> the underlying raw type of the identifier (typically {@link java.util.UUID})
 */
public interface Identifier<T> extends ValueObject {

    /** @return the underlying raw value of this identifier */
    T value();
}
