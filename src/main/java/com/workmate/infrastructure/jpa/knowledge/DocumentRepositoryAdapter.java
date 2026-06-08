package com.workmate.infrastructure.jpa.knowledge;

import com.workmate.domain.knowledge.Document;
import com.workmate.domain.knowledge.DocumentId;
import com.workmate.domain.knowledge.DocumentRepository;
import com.workmate.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;
    private final DocumentMapper mapper;

    public DocumentRepositoryAdapter(DocumentJpaRepository jpaRepository, DocumentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(DocumentId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Document save(Document document) {
        // Persist via the JPA mapping, but return the ORIGINAL aggregate so its pending
        // domain events survive for the application service to publish. Re-mapping through
        // toDomain() would yield a reconstituted instance with an empty event buffer.
        jpaRepository.save(mapper.toEntity(document));
        return document;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByWorkspace(WorkspaceId workspaceId) {
        return jpaRepository.findByWorkspaceId(workspaceId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
