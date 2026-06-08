package com.workmate.interfaces.rest;

import com.workmate.application.chat.ChatService;
import com.workmate.application.chat.ConversationView;
import com.workmate.application.chat.GetConversationQuery;
import com.workmate.application.chat.SendMessageCommand;
import com.workmate.application.chat.SendMessageResult;
import com.workmate.interfaces.rest.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller for chat operations.
 *
 * <p>POST /api/v1/chat/messages streams the assistant reply as Server-Sent Events.
 * GET /api/v1/conversations/{id} returns the full conversation view.
 *
 * <p>TODO Phase 2: replace token-splitting stub with real AgentLoop/Claude streaming.
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final Duration TOKEN_INTERVAL = Duration.ofMillis(40);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
    }

    /**
     * Send a message and stream the assistant reply token-by-token via SSE.
     *
     * <p>The blocking {@link ChatService#handle(SendMessageCommand)} call is offloaded to
     * {@code boundedElastic()} so the event-loop thread is never blocked. The last
     * ASSISTANT message content is then split on spaces and emitted with a 40 ms delay
     * between tokens to simulate streaming. A final {@code done} event carries the
     * conversation ID.
     *
     * <p>TODO Phase 2: stream real tokens from AgentLoop/Claude instead of splitting content.
     *
     * @param workspaceId the tenant workspace UUID from {@code X-Workspace-Id} header
     * @param request     the validated request body
     * @return a reactive SSE stream of token strings followed by a {@code done} event
     */
    @PostMapping(value = "/chat/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessage(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @Valid @RequestBody SendMessageRequest request) {

        log.debug("sendMessage workspaceId={} conversationId={}", workspaceId,
                request.conversationId());

        SendMessageCommand cmd = new SendMessageCommand(
                workspaceId, request.conversationId(), request.content());

        return Mono.fromCallable(() -> chatService.handle(cmd))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(result -> streamTokens(result));
    }

    /**
     * Retrieve a conversation with its full message history.
     *
     * @param workspaceId    the tenant workspace UUID from {@code X-Workspace-Id} header
     * @param conversationId the conversation to retrieve
     * @return the conversation view
     */
    @GetMapping("/conversations/{id}")
    public Mono<ConversationView> getConversation(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable("id") UUID conversationId) {

        log.debug("getConversation workspaceId={} conversationId={}", workspaceId, conversationId);

        GetConversationQuery query = new GetConversationQuery(workspaceId, conversationId);
        return Mono.fromCallable(() -> chatService.handle(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // --- private helpers ---

    /**
     * Split the last ASSISTANT message content into space-delimited tokens and emit each
     * as a {@code message} SSE event with a configurable delay, followed by a {@code done}
     * event carrying the conversation ID.
     */
    private Flux<ServerSentEvent<String>> streamTokens(SendMessageResult result) {
        String assistantContent = result.messages().stream()
                .filter(m -> "ASSISTANT".equals(m.role()))
                .reduce((first, second) -> second) // last assistant message
                .map(m -> m.content())
                .orElse("");

        List<String> tokens = Arrays.asList(assistantContent.split(" "));

        AtomicInteger index = new AtomicInteger(0);

        Flux<ServerSentEvent<String>> tokenFlux = Flux.interval(TOKEN_INTERVAL)
                .take(tokens.size())
                .map(tick -> {
                    String token = tokens.get(index.getAndIncrement());
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(token)
                            .build();
                });

        ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder()
                .event("done")
                .data(result.conversationId().toString())
                .build();

        return tokenFlux.concatWith(Flux.just(doneEvent));
    }
}
