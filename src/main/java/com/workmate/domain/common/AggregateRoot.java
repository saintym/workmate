package com.workmate.domain.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for Aggregate Roots — the single entry point and consistency boundary for a
 * cluster of domain objects.
 *
 * <p>External code may only hold references to and mutate state through the root. The root
 * records {@link DomainEvent}s as side effects of behaviour; the application layer pulls
 * and publishes them after the transaction commits via {@link #pullDomainEvents()}.
 *
 * <p>Pure Java — the domain layer has ZERO Spring/framework dependencies.
 *
 * @param <ID> the typed identifier of this aggregate root
 */
public abstract class AggregateRoot<ID extends Identifier<?>> extends Entity<ID> {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        super(id);
    }

    /** Record a domain event to be published after the aggregate is persisted. */
    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    /** @return an unmodifiable snapshot of currently pending events (without clearing). */
    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(new ArrayList<>(domainEvents));
    }

    /**
     * Drain pending events: returns the accumulated events and clears the internal buffer.
     * Typically called by the persistence/application layer right after a successful save.
     *
     * @return the events that had accumulated since the last drain
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> drained = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(drained);
    }
}
