package com.workmate.domain.knowledge;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

import java.util.Arrays;

/**
 * A dense vector embedding produced by an embedding model.
 *
 * <p>The default dimension is 1536, matching OpenAI text-embedding-3-small and compatible
 * with pgvector's {@code vector(1536)} column type. The infrastructure layer stores this
 * via pgvector; the domain layer treats it as an opaque float array.
 *
 * <p>Array equality is handled explicitly because Java records use reference equality for
 * arrays by default.
 */
public record Embedding(float[] vector) implements ValueObject {

    public Embedding {
        if (vector == null || vector.length == 0) {
            throw new DomainException("Embedding vector must not be null or empty");
        }
        vector = Arrays.copyOf(vector, vector.length);
    }

    /**
     * @return the dimensionality of this embedding vector
     */
    public int dimension() {
        return vector.length;
    }

    /**
     * Returns a defensive copy of the underlying float array.
     */
    @Override
    public float[] vector() {
        return Arrays.copyOf(vector, vector.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Embedding other = (Embedding) o;
        return Arrays.equals(vector, other.vector);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vector);
    }

    @Override
    public String toString() {
        return "Embedding{dim=" + vector.length + "}";
    }
}
