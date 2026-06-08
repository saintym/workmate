package com.workmate.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.application.document.DocumentIndexingService;
import com.workmate.domain.knowledge.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Kafka consumer that triggers RAG indexing when a document is uploaded.
 *
 * <p>Listens to the {@code domain.events} topic and dispatches
 * {@code DocumentUploadedEvent} messages to {@link DocumentIndexingService#index}.
 * All other event types are silently skipped. Exceptions are caught and logged so a
 * poison message never kills the listener partition assignment.
 *
 * <p>TODO: add retry + dead-letter topic support for transient failures.
 */
@Component
public class KafkaDocumentIndexingConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaDocumentIndexingConsumer.class);
    private static final String DOCUMENT_UPLOADED_KEY = "DocumentUploadedEvent";

    private final DocumentIndexingService documentIndexingService;
    private final ObjectMapper objectMapper;

    public KafkaDocumentIndexingConsumer(DocumentIndexingService documentIndexingService,
                                          ObjectMapper objectMapper) {
        this.documentIndexingService = Objects.requireNonNull(documentIndexingService,
                "documentIndexingService must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper,
                "objectMapper must not be null");
    }

    /**
     * Receives a raw JSON string from the {@code domain.events} topic and, if the message
     * key is {@code DocumentUploadedEvent}, triggers document indexing.
     *
     * @param payload the JSON-encoded domain event string
     * @param key     the Kafka message key (event class simple name)
     */
    @KafkaListener(topics = "domain.events", groupId = "workmate-indexing")
    public void onDomainEvent(String payload,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        if (!DOCUMENT_UPLOADED_KEY.equals(key)) {
            log.debug("KafkaDocumentIndexingConsumer: ignoring event key={}", key);
            return;
        }

        log.info("KafkaDocumentIndexingConsumer: received DocumentUploadedEvent");
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode documentIdNode = root.path("documentId").path("value");
            if (documentIdNode.isMissingNode() || documentIdNode.isNull()) {
                log.error("KafkaDocumentIndexingConsumer: missing documentId.value in payload: {}",
                        payload);
                return;
            }
            UUID documentUuid = UUID.fromString(documentIdNode.asText());
            DocumentId documentId = DocumentId.of(documentUuid);
            log.info("KafkaDocumentIndexingConsumer: triggering indexing for documentId={}",
                    documentId);
            documentIndexingService.index(documentId);
        } catch (Exception e) {
            // Catch all exceptions to prevent partition rebalance / consumer death.
            // TODO: route to dead-letter topic after N retries.
            log.error("KafkaDocumentIndexingConsumer: failed to process DocumentUploadedEvent "
                    + "payload={}", payload, e);
        }
    }
}
