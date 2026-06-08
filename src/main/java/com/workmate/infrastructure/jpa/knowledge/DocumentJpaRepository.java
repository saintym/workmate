package com.workmate.infrastructure.jpa.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    List<DocumentJpaEntity> findByWorkspaceId(UUID workspaceId);
}
