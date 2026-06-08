package com.workmate.domain.common;

/**
 * Port interface for publishing domain events out of the domain layer.
 *
 * <p>Implementations live in the infrastructure layer (e.g. Spring ApplicationEventPublisher,
 * an in-process bus, or a message broker adapter). The domain layer depends only on this
 * interface — it carries no framework imports.
 *
 * <p>Pure Java — no framework dependencies.
 */
public interface EventPublisher {

    /**
     * Publish a single domain event.
     *
     * @param event the event to publish; must not be {@code null}
     */
    void publish(DomainEvent event);

    /**
     * Convenience method to publish all events collected on an aggregate root.
     *
     * @param events the events to publish; must not be {@code null}
     */
    default void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
