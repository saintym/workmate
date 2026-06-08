package com.workmate.application.chat;

import com.workmate.application.common.ResourceNotFoundException;
import com.workmate.domain.agent.AgentException;
import com.workmate.domain.agent.service.AgentLoop;
import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.EventPublisher;
import com.workmate.domain.conversation.Conversation;
import com.workmate.domain.conversation.ConversationId;
import com.workmate.domain.conversation.ConversationRepository;
import com.workmate.domain.conversation.Message;
import com.workmate.domain.conversation.MessageContent;
import com.workmate.domain.conversation.MessageRole;
import com.workmate.domain.workspace.WorkspaceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service handling chat CQRS use cases.
 *
 * <p>Coordinates the {@link ConversationRepository}, {@link EventPublisher}, and
 * {@link AgentLoop} ports. All domain logic stays in the {@link Conversation} aggregate
 * and the agent loop; this class is purely orchestration.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;
    private final AgentLoop agentLoop;

    public ChatService(ConversationRepository conversationRepository,
                       EventPublisher eventPublisher,
                       AgentLoop agentLoop) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository,
                "conversationRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher,
                "eventPublisher must not be null");
        this.agentLoop = Objects.requireNonNull(agentLoop,
                "agentLoop must not be null");
    }

    /**
     * Handle a {@link SendMessageCommand}.
     *
     * <p>When {@code cmd.conversationId()} is {@code null} a new conversation is started in
     * the given workspace. Otherwise the existing conversation is loaded and the workspace
     * ownership is verified for tenant isolation. The user message is appended, then the
     * {@link AgentLoop} runs: it may invoke tools and appends the final {@code ASSISTANT}
     * message to the conversation. The aggregate is then persisted and domain events are
     * published.
     *
     * <p>If the agent loop raises an {@link AgentException} (e.g.
     * {@link com.workmate.domain.agent.MaxIterationsExceededException}), a friendly Korean
     * fallback message is appended so the user always receives a reply and the conversation
     * is still persisted.
     *
     * @param cmd the send-message command; must not be null
     * @return the updated conversation with all messages
     * @throws DomainException           if the conversation belongs to a different workspace
     * @throws ResourceNotFoundException if a non-null conversationId does not exist
     */
    @Transactional
    public SendMessageResult handle(SendMessageCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        Conversation conversation;
        if (cmd.conversationId() == null) {
            conversation = Conversation.start(WorkspaceId.of(cmd.workspaceId()));
        } else {
            conversation = conversationRepository
                    .findById(ConversationId.of(cmd.conversationId()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversation not found: " + cmd.conversationId()));
            guardWorkspaceMatch(conversation, cmd.workspaceId());
        }

        conversation.addMessage(MessageRole.USER, MessageContent.of(cmd.content()));

        try {
            agentLoop.run(conversation);
        } catch (AgentException ex) {
            log.error("Agent loop failed for conversation {}: {}", conversation.id(), ex.getMessage(), ex);
            conversation.addMessage(MessageRole.ASSISTANT,
                    MessageContent.of("죄송합니다. 요청을 처리하지 못했습니다."));
        }

        conversation = conversationRepository.save(conversation);
        eventPublisher.publishAll(conversation.pullDomainEvents());

        return toSendMessageResult(conversation);
    }

    /**
     * Handle a {@link GetConversationQuery}.
     *
     * <p>Loads the conversation, verifies workspace ownership, and maps to a view.
     *
     * @param query the query; must not be null
     * @return the conversation view
     * @throws DomainException           if the conversation belongs to a different workspace
     * @throws ResourceNotFoundException if the conversation does not exist
     */
    @Transactional(readOnly = true)
    public ConversationView handle(GetConversationQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Conversation conversation = conversationRepository
                .findById(ConversationId.of(query.conversationId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found: " + query.conversationId()));

        guardWorkspaceMatch(conversation, query.workspaceId());

        return toConversationView(conversation);
    }

    // --- private helpers ---

    /**
     * Verify that {@code conversation} belongs to the given workspace.
     *
     * @throws DomainException if workspace IDs do not match (tenant isolation guard)
     */
    private void guardWorkspaceMatch(Conversation conversation, UUID workspaceId) {
        if (!conversation.workspaceId().value().equals(workspaceId)) {
            throw new DomainException(
                    "Conversation " + conversation.id() + " does not belong to workspace " + workspaceId);
        }
    }

    /** Map a {@link Message} to its {@link MessageView} read model. */
    private MessageView toMessageView(Message message) {
        return new MessageView(
                message.role().name(),
                message.content().value(),
                message.sequence(),
                message.createdAt());
    }

    /** Map a {@link Conversation} to a {@link SendMessageResult}. */
    private SendMessageResult toSendMessageResult(Conversation conversation) {
        List<MessageView> views = conversation.messages().stream()
                .map(this::toMessageView)
                .toList();
        return new SendMessageResult(conversation.id().value(), views);
    }

    /** Map a {@link Conversation} to a {@link ConversationView}. */
    private ConversationView toConversationView(Conversation conversation) {
        List<MessageView> views = conversation.messages().stream()
                .map(this::toMessageView)
                .toList();
        return new ConversationView(
                conversation.id().value(),
                conversation.workspaceId().value(),
                conversation.status().name(),
                views);
    }
}
