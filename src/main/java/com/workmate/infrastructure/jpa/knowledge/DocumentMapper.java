package com.workmate.infrastructure.jpa.knowledge;

import com.workmate.domain.knowledge.ChunkId;
import com.workmate.domain.knowledge.ChunkIndex;
import com.workmate.domain.knowledge.Document;
import com.workmate.domain.knowledge.DocumentChunk;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.DocumentName;
import com.workmate.domain.knowledge.DocumentStatus;
import com.workmate.domain.knowledge.Embedding;
import com.workmate.domain.knowledge.S3Key;
import com.workmate.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Component;

import java.util.List;

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

        List<DocumentChunkJpaEntity> chunkEntities = domain.chunks().stream()
                .map(c -> toChunkEntity(c, entity))
                .toList();
        entity.getChunks().clear();
        entity.getChunks().addAll(chunkEntities);

        return entity;
    }

    private DocumentChunkJpaEntity toChunkEntity(DocumentChunk domain, DocumentJpaEntity documentEntity) {
        DocumentChunkJpaEntity entity = new DocumentChunkJpaEntity();
        entity.setId(domain.id().value());
        entity.setDocument(documentEntity);
        entity.setChunkIndex(domain.index().value());
        entity.setContent(domain.content());
        entity.setEmbedding(domain.embedding() != null ? domain.embedding().vector() : null);
        return entity;
    }

    public Document toDomain(DocumentJpaEntity entity) {
        List<DocumentChunk> chunks = entity.getChunks().stream()
                .map(this::toChunkDomain)
                .toList();

        return Document.reconstitute(
                DocumentId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                DocumentName.of(entity.getName()),
                S3Key.of(entity.getS3Key()),
                DocumentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                chunks
        );
    }

    private DocumentChunk toChunkDomain(DocumentChunkJpaEntity entity) {
        float[] raw = entity.getEmbedding();
        Embedding embedding = (raw != null && raw.length > 0) ? new Embedding(raw) : null;

        return DocumentChunk.reconstitute(
                ChunkId.of(entity.getId()),
                ChunkIndex.of(entity.getChunkIndex()),
                entity.getContent(),
                embedding
        );
    }
}
