package com.workmate.application.document;

import com.workmate.application.port.DocumentStorage;
import com.workmate.domain.knowledge.ChunkIndex;
import com.workmate.domain.knowledge.ChunkingService;
import com.workmate.domain.knowledge.ChunkingStrategy;
import com.workmate.domain.knowledge.Document;
import com.workmate.domain.knowledge.DocumentChunk;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.DocumentRepository;
import com.workmate.domain.knowledge.Embedding;
import com.workmate.domain.knowledge.EmbeddingService;
import com.workmate.domain.knowledge.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service that drives the full RAG indexing pipeline for a single document.
 *
 * <p>Lifecycle: loads the document, transitions it to {@code INDEXING}, chunks its text,
 * generates embeddings, saves vectors, then marks it {@code INDEXED}. On any error the
 * document is marked {@code FAILED} and the exception is swallowed so the Kafka consumer
 * can decide retry behaviour independently.
 *
 * <p>The operation is idempotent: {@link VectorSearchRepository#deleteByDocument} purges
 * any previously stored chunks before the new ones are written.
 */
@Service
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorSearchRepository vectorSearchRepository;

    public DocumentIndexingService(DocumentRepository documentRepository,
                                   DocumentStorage documentStorage,
                                   ChunkingService chunkingService,
                                   EmbeddingService embeddingService,
                                   VectorSearchRepository vectorSearchRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository,
                "documentRepository must not be null");
        this.documentStorage = Objects.requireNonNull(documentStorage,
                "documentStorage must not be null");
        this.chunkingService = Objects.requireNonNull(chunkingService,
                "chunkingService must not be null");
        this.embeddingService = Objects.requireNonNull(embeddingService,
                "embeddingService must not be null");
        this.vectorSearchRepository = Objects.requireNonNull(vectorSearchRepository,
                "vectorSearchRepository must not be null");
    }

    /**
     * Index a document: chunk its text, embed each chunk, and persist the vectors.
     *
     * <p>On success the document transitions {@code UPLOADED → INDEXING → INDEXED}.
     * On any failure the document transitions to {@code FAILED}; the exception is logged
     * and never rethrown so the Kafka listener is not disturbed.
     *
     * @param documentId the identifier of the document to index; must not be null
     */
    @Transactional
    public void index(DocumentId documentId) {
        Objects.requireNonNull(documentId, "documentId must not be null");

        Optional<Document> maybeDoc = documentRepository.findById(documentId);
        if (maybeDoc.isEmpty()) {
            log.warn("DocumentIndexingService.index: document not found, skipping. documentId={}",
                    documentId);
            return;
        }

        Document document = maybeDoc.get();
        try {
            document.markIndexing();
            documentRepository.save(document);

            String text = documentStorage.load(document.s3Key().value());

            List<String> chunkTexts = chunkingService.chunk(text, ChunkingStrategy.defaultStrategy());
            log.info("Chunked documentId={} into {} chunks", documentId, chunkTexts.size());

            // Purge any previously stored vectors (idempotency)
            vectorSearchRepository.deleteByDocument(documentId);

            List<DocumentChunk> chunks = new ArrayList<>(chunkTexts.size());
            for (int i = 0; i < chunkTexts.size(); i++) {
                String chunkText = chunkTexts.get(i);
                DocumentChunk chunk = DocumentChunk.create(new ChunkIndex(i), chunkText);
                Embedding embedding = embeddingService.embed(chunkText);
                chunk.attachEmbedding(embedding);
                chunks.add(chunk);
            }

            vectorSearchRepository.saveChunks(document.workspaceId(), documentId, chunks);

            document.markIndexed();
            documentRepository.save(document);

            log.info("Indexed documentId={} successfully ({} chunks)", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to index documentId={}: {}", documentId, e.getMessage(), e);
            try {
                document.markFailed();
                documentRepository.save(document);
            } catch (Exception saveEx) {
                log.error("Failed to persist FAILED status for documentId={}", documentId, saveEx);
            }
        }
    }
}
