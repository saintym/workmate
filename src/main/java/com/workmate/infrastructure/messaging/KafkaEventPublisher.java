package com.workmate.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.domain.common.DomainEvent;
import com.workmate.domain.common.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter for the {@link EventPublisher} port.
 *
 * <p>Serializes each {@link DomainEvent} to JSON here (rather than relying on a generic
 * value serializer) so the wire format is owned by infrastructure and the pure-Java domain
 * events — which expose fluent accessors like {@code eventId()} instead of {@code getX()}
 * getters — need no Jackson-specific shaping. A copied {@link ObjectMapper} with field-level
 * visibility reads their private fields directly.
 *
 * <p>Publishing is best-effort: a serialization or broker failure is logged but never
 * propagated, so it cannot roll back the business transaction that produced the event.
 * TODO Phase 3: per-event topic routing (e.g. {@code document.uploaded}) and an outbox for
 * guaranteed delivery.
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "domain.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        // Field visibility lets Jackson serialize the domain events (no getters) without
        // mutating the application-wide ObjectMapper.
        this.objectMapper = objectMapper.copy()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public void publish(DomainEvent event) {
        String key = event.getClass().getSimpleName();
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("Publishing domain event: topic={}, key={}, eventId={}", TOPIC, key, event.eventId());
            kafkaTemplate.send(TOPIC, key, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event {} (eventId={}); dropping", key, event.eventId(), e);
        } catch (RuntimeException e) {
            log.error("Failed to publish domain event {} (eventId={}) to Kafka; dropping", key, event.eventId(), e);
        }
    }
}
