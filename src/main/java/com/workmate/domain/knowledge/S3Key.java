package com.workmate.domain.knowledge;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * The S3 object key that locates the raw document file in object storage.
 */
public record S3Key(String value) implements ValueObject {

    public S3Key {
        if (value == null || value.isBlank()) {
            throw new DomainException("S3Key must not be blank");
        }
    }

    public static S3Key of(String value) {
        return new S3Key(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
