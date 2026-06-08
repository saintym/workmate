package com.workmate.domain.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java domain service that splits a document's text into overlapping character
 * windows according to a {@link ChunkingStrategy}.
 *
 * <p>The algorithm is a simple sliding window over raw characters:
 * <ul>
 *   <li>Window width  = {@code strategy.chunkSize()}</li>
 *   <li>Step size     = {@code strategy.chunkSize() - strategy.overlap()}</li>
 * </ul>
 * Consecutive chunks therefore share {@code overlap} trailing/leading characters,
 * giving the retriever context around chunk boundaries.
 *
 * <p><b>Note:</b> splitting is character-based, which is a pragmatic approximation of
 * token windows. Token-aware (e.g. tiktoken) or sentence-aware splitting is a future
 * refinement and can be substituted without changing the domain interface.
 */
public class ChunkingService {

    /**
     * Splits {@code text} into overlapping character windows.
     *
     * @param text     the document text to chunk; {@code null} is treated as blank
     * @param strategy the chunking parameters; must not be null
     * @return an unmodifiable list of text chunks, empty when {@code text} is blank
     */
    public List<String> chunk(String text, ChunkingStrategy strategy) {
        if (strategy == null) {
            throw new com.workmate.domain.common.DomainException("ChunkingStrategy must not be null");
        }

        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String trimmed = text.trim();
        int chunkSize = strategy.chunkSize();
        int step = chunkSize - strategy.overlap();

        if (trimmed.length() <= chunkSize) {
            return List.of(trimmed);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < trimmed.length()) {
            int end = Math.min(start + chunkSize, trimmed.length());
            chunks.add(trimmed.substring(start, end));
            if (end == trimmed.length()) {
                break;
            }
            start += step;
        }
        return Collections.unmodifiableList(chunks);
    }
}
