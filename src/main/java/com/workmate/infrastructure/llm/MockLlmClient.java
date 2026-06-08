package com.workmate.infrastructure.llm;

import com.workmate.domain.agent.LlmClient;
import com.workmate.domain.agent.LlmMessage;
import com.workmate.domain.agent.LlmRequest;
import com.workmate.domain.agent.LlmResponse;
import com.workmate.domain.agent.ToolCall;
import com.workmate.domain.agent.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic {@link LlmClient} for tests and offline demos.
 *
 * <p>Activated when {@code app.llm.provider=mock}. Makes no external calls.
 * Covers three scenarios:
 * <ol>
 *   <li>A TOOL message is already in the transcript → summarise the result.</li>
 *   <li>A {@code query_database} tool is available and the last user message mentions
 *       business keywords → return a tool-call request.</li>
 *   <li>Otherwise → return a generic greeting.</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock")
public class MockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    private static final String QUERY_DATABASE_TOOL = "query_database";
    private static final String BUSINESS_KEYWORD_REGEX =
            ".*(?i)(환불|매출|주문|refund|sales|order).*";

    @Override
    public LlmResponse complete(LlmRequest request) {
        log.debug("MockLlmClient.complete called (messages={}, tools={})",
                request.messages().size(), request.tools().size());

        // Case 1: a TOOL result is already in the transcript → summarise it
        String lastToolContent = findLastToolContent(request.messages());
        if (lastToolContent != null) {
            String summary = "(mock) 도구 결과를 받았습니다: "
                    + lastToolContent.trim().substring(0, Math.min(lastToolContent.trim().length(), 200));
            log.debug("MockLlmClient: returning tool-result summary");
            return LlmResponse.ofText(summary);
        }

        // Case 2: query_database available + last user message has business keywords
        boolean hasQueryDatabase = request.tools().stream()
                .map(ToolDefinition::name)
                .anyMatch(QUERY_DATABASE_TOOL::equals);

        if (hasQueryDatabase) {
            String lastUserContent = findLastUserContent(request.messages());
            if (lastUserContent != null && lastUserContent.matches(BUSINESS_KEYWORD_REGEX)) {
                log.debug("MockLlmClient: returning query_database tool call");
                return LlmResponse.ofToolCalls(List.of(
                        new ToolCall(QUERY_DATABASE_TOOL,
                                "{\"sql\":\"SELECT name FROM products LIMIT 1\"}")));
            }
        }

        // Case 3: default greeting
        log.debug("MockLlmClient: returning default greeting");
        return LlmResponse.ofText("(mock) 안녕하세요, 무엇을 도와드릴까요?");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String findLastToolContent(List<LlmMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("TOOL".equalsIgnoreCase(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }

    private String findLastUserContent(List<LlmMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("USER".equalsIgnoreCase(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }
}
