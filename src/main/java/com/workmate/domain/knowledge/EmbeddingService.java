package com.workmate.domain.knowledge;

import java.util.List;

/**
 * Port interface (driven port) for computing dense vector embeddings from text.
 *
 * <p>Implementations live in the infrastructure layer:
 * <ul>
 *   <li>{@code infrastructure/llm/HashingEmbeddingService} — deterministic local hashing
 *       embedder used as the default (no external API call, zero cost, good for tests)</li>
 *   <li>{@code infrastructure/llm/OpenAiEmbeddingService} — delegates to
 *       {@code text-embedding-3-small}; wired in when {@code OPENAI_API_KEY} is present</li>
 * </ul>
 *
 * <p>Pure Java — no Spring/framework imports.
 */
public interface EmbeddingService {

    /**
     * Produces a dense vector embedding for the given text.
     *
     * @param text the text to embed; must not be null or blank
     * @return an {@link Embedding} whose {@link Embedding#dimension()} matches
     *         {@link #dimension()}
     */
    Embedding embed(String text);

    /**
     * Embeds a batch of texts sequentially. Override in implementations that support
     * true batch API calls to improve throughput.
     *
     * @param texts the texts to embed; must not be null; individual elements must not be null
     * @return an unmodifiable list of embeddings in the same order as {@code texts}
     */
    default List<Embedding> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    /**
     * Returns the fixed vector dimensionality produced by this service.
     *
     * @return vector dimension, e.g. 1536 for {@code text-embedding-3-small}
     */
    int dimension();
}
