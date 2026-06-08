package com.workmate.infrastructure.jpa.workspace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.domain.workspace.Workspace;
import com.workmate.domain.workspace.WorkspaceId;
import com.workmate.domain.workspace.WorkspaceName;
import com.workmate.domain.workspace.WorkspaceSettings;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkspaceMapper {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public WorkspaceJpaEntity toEntity(Workspace domain) {
        WorkspaceJpaEntity entity = new WorkspaceJpaEntity();
        entity.setId(domain.id().value());
        entity.setName(domain.name().value());
        entity.setSettings(serializeSettings(domain.settings()));
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public Workspace toDomain(WorkspaceJpaEntity entity) {
        return Workspace.reconstitute(
                WorkspaceId.of(entity.getId()),
                WorkspaceName.of(entity.getName()),
                deserializeSettings(entity.getSettings()),
                entity.getCreatedAt()
        );
    }

    private String serializeSettings(WorkspaceSettings settings) {
        try {
            return OBJECT_MAPPER.writeValueAsString(settings.values());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize WorkspaceSettings", e);
        }
    }

    private WorkspaceSettings deserializeSettings(String json) {
        if (json == null || json.isBlank()) {
            return WorkspaceSettings.empty();
        }
        try {
            Map<String, String> map = OBJECT_MAPPER.readValue(json, MAP_TYPE);
            return new WorkspaceSettings(map);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize WorkspaceSettings", e);
        }
    }
}
