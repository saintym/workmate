package com.workmate.domain.workspace;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-tenant configuration (model preferences, feature flags, etc.) held as an immutable
 * key/value map. Kept deliberately schemaless for the toy project; persisted as JSON.
 */
public record WorkspaceSettings(Map<String, String> values) implements ValueObject {

    public WorkspaceSettings {
        if (values == null) {
            throw new DomainException("WorkspaceSettings values must not be null");
        }
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static WorkspaceSettings empty() {
        return new WorkspaceSettings(Map.of());
    }

    public WorkspaceSettings with(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(this.values);
        next.put(key, value);
        return new WorkspaceSettings(next);
    }

    public String get(String key) {
        return values.get(key);
    }
}
