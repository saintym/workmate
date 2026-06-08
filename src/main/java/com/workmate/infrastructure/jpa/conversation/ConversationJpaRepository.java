package com.workmate.infrastructure.jpa.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    List<ConversationJpaEntity> findByWorkspaceId(UUID workspaceId);
}
