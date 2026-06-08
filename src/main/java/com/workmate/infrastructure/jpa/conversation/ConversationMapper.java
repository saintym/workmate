package com.workmate.infrastructure.jpa.conversation;

import com.workmate.domain.conversation.Conversation;
import com.workmate.domain.conversation.ConversationId;
import com.workmate.domain.conversation.ConversationStatus;
import com.workmate.domain.conversation.Message;
import com.workmate.domain.conversation.MessageContent;
import com.workmate.domain.conversation.MessageId;
import com.workmate.domain.conversation.MessageRole;
import com.workmate.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationMapper {

    public ConversationJpaEntity toEntity(Conversation domain) {
        ConversationJpaEntity entity = new ConversationJpaEntity();
        entity.setId(domain.id().value());
        entity.setWorkspaceId(domain.workspaceId().value());
        entity.setStatus(domain.status().name());
        entity.setCreatedAt(domain.createdAt());

        List<MessageJpaEntity> messageEntities = domain.messages().stream()
                .map(m -> toMessageEntity(m, entity))
                .toList();
        entity.getMessages().clear();
        entity.getMessages().addAll(messageEntities);

        return entity;
    }

    private MessageJpaEntity toMessageEntity(Message domain, ConversationJpaEntity conversationEntity) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setId(domain.id().value());
        entity.setConversation(conversationEntity);
        entity.setRole(domain.role().name());
        entity.setContent(domain.content().value());
        entity.setSequence(domain.sequence());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public Conversation toDomain(ConversationJpaEntity entity) {
        List<Message> messages = entity.getMessages().stream()
                .map(this::toMessageDomain)
                .toList();

        return Conversation.reconstitute(
                ConversationId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                ConversationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                messages
        );
    }

    private Message toMessageDomain(MessageJpaEntity entity) {
        return Message.reconstitute(
                MessageId.of(entity.getId()),
                MessageRole.valueOf(entity.getRole()),
                new MessageContent(entity.getContent()),
                entity.getSequence(),
                entity.getCreatedAt()
        );
    }
}
