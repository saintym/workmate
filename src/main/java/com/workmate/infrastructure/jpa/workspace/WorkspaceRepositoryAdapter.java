package com.workmate.infrastructure.jpa.workspace;

import com.workmate.domain.workspace.Workspace;
import com.workmate.domain.workspace.WorkspaceId;
import com.workmate.domain.workspace.WorkspaceRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class WorkspaceRepositoryAdapter implements WorkspaceRepository {

    private final WorkspaceJpaRepository jpaRepository;
    private final WorkspaceMapper mapper;

    public WorkspaceRepositoryAdapter(WorkspaceJpaRepository jpaRepository, WorkspaceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workspace> findById(WorkspaceId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Workspace save(Workspace workspace) {
        // Persist via the JPA mapping, but return the ORIGINAL aggregate so its pending
        // domain events survive for the application service to publish. Re-mapping through
        // toDomain() would yield a reconstituted instance with an empty event buffer.
        jpaRepository.save(mapper.toEntity(workspace));
        return workspace;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(WorkspaceId id) {
        return jpaRepository.existsById(id.value());
    }
}
