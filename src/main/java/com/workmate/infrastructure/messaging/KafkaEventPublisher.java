package com.workmate.infrastructure.messaging;

import com.workmate.domain.common.DomainEvent;
import com.workmate.domain.common.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "domain.events";

    // TODO Phase 3: per-event topic routing (document.uploaded)

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        String key = event.getClass().getSimpleName();
        log.info("Publishing domain event: topic={}, key={}, eventId={}", TOPIC, key, event.eventId());
        kafkaTemplate.send(TOPIC, key, event);
    }
}
