package com.workmate.infrastructure.jpa.knowledge;

import com.workmate.domain.knowledge.Document;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.DocumentName;
import com.workmate.domain.knowledge.DocumentStatus;
import com.workmate.domain.knowledge.S3Key;
import com.workmate.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between {@link DocumentJpaEntity} and the {@link Document} domain aggregate.
 *
 * <p>Chunk content is no longer loaded through JPA. Document chunks live exclusively
 * in the {@code document_chunks} table managed by
 * {@code infrastructure.persistence.pgvector.VectorSearchRepositoryAdapter} via
 * raw JDBC + pgvector. Reconstituted documents therefore carry an empty chunk list —
 * callers that need chunks must query the vector search repository directly.
 */
@Component
public class DocumentMapper {

    public DocumentJpaEntity toEntity(Document domain) {
        DocumentJpaEntity entity = new DocumentJpaEntity();
        entity.setId(domain.id().value());
        entity.setWorkspaceId(domain.workspaceId().value());
        entity.setName(domain.name().value());
        entity.setS3Key(domain.s3Key().value());
        entity.setStatus(domain.status().name());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public Document toDomain(DocumentJpaEntity entity) {
        return Document.reconstitute(
                DocumentId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                DocumentName.of(entity.getName()),
                S3Key.of(entity.getS3Key()),
                DocumentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                List.of()
        );
    }
}
