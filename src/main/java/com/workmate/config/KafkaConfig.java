package com.workmate.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka infrastructure configuration.
 *
 * <p>Declares the {@code domain.events} topic used for publishing domain events from all
 * bounded contexts (workspace, conversation, knowledge, agent, database). The topic is
 * created automatically on application start if it does not already exist, thanks to
 * Spring Boot's {@code KafkaAdmin} auto-configuration.
 *
 * <p>Producer and consumer serialization settings are provided via {@code application.yml}
 * (Spring Boot auto-config). This class only handles topic topology.
 */
@Configuration
public class KafkaConfig {

    /**
     * The central domain-events topic.
     *
     * <p>Single partition and replication factor of 1 are appropriate for local development.
     * Increase both values for staging / production deployments.
     */
    @Bean
    public NewTopic domainEventsTopic() {
        return TopicBuilder.name("domain.events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
