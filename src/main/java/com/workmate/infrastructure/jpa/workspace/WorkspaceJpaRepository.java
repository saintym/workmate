package com.workmate.infrastructure.jpa.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, UUID> {
}
