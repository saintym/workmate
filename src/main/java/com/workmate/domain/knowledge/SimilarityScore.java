package com.workmate.domain.knowledge;

import com.workmate.domain.common.ValueObject;

/**
 * Cosine similarity score in the range [0.0, 1.0] between a query embedding and a
 * stored {@link DocumentChunk} embedding.
 *
 * <p>Raw cosine similarity can drift slightly outside [0, 1] due to floating-point
 * rounding in the vector store. This record clamps the incoming value to [0.0, 1.0]
 * rather than throwing, so callers are not burdened by inconsequential fp noise.
 */
public record SimilarityScore(double value) implements ValueObject {

    public SimilarityScore {
        value = Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Factory method — clamps {@code rawScore} to [0.0, 1.0].
     *
     * @param rawScore the raw cosine similarity, possibly slightly outside [0, 1]
     * @return a {@code SimilarityScore} with the clamped value
     */
    public static SimilarityScore of(double rawScore) {
        return new SimilarityScore(rawScore);
    }

    @Override
    public String toString() {
        return "SimilarityScore{" + value + "}";
    }
}
