package com.workmate.domain.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Convenience base for {@link DomainEvent} implementations.
 *
 * <p>Stamps {@code eventId} and {@code occurredAt} at construction so concrete events only
 * carry their own payload. Pure Java — no framework dependencies.
 */
public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
