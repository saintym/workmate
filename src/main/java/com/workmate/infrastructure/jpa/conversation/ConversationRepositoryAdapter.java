package com.workmate.infrastructure.jpa.conversation;

import com.workmate.domain.conversation.Conversation;
import com.workmate.domain.conversation.ConversationId;
import com.workmate.domain.conversation.ConversationRepository;
import com.workmate.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationJpaRepository jpaRepository;
    private final ConversationMapper mapper;

    public ConversationRepositoryAdapter(ConversationJpaRepository jpaRepository,
                                         ConversationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(ConversationId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity saved = jpaRepository.save(mapper.toEntity(conversation));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findByWorkspace(WorkspaceId workspaceId) {
        return jpaRepository.findByWorkspaceId(workspaceId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
