package com.workmate.domain.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Something meaningful that happened in the domain, expressed in past tense
 * (e.g. {@code DocumentUploadedEvent}).
 *
 * <p>Events are raised by aggregates and published after the transaction commits.
 * Pure Java — no framework dependencies.
 */
public interface DomainEvent {

    /** @return a unique id for this event occurrence */
    UUID eventId();

    /** @return when the event occurred */
    Instant occurredAt();
}
