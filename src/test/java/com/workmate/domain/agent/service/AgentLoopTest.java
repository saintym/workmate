package com.workmate.domain.agent.service;

import com.workmate.domain.agent.AgentResult;
import com.workmate.domain.agent.LlmClient;
import com.workmate.domain.agent.LlmRequest;
import com.workmate.domain.agent.LlmResponse;
import com.workmate.domain.agent.MaxIterationsExceededException;
import com.workmate.domain.agent.MaxIterationsPolicy;
import com.workmate.domain.agent.Tool;
import com.workmate.domain.agent.ToolCall;
import com.workmate.domain.agent.ToolDefinition;
import com.workmate.domain.agent.ToolNotFoundException;
import com.workmate.domain.agent.ToolRegistry;
import com.workmate.domain.agent.ToolResult;
import com.workmate.domain.conversation.Conversation;
import com.workmate.domain.conversation.MessageContent;
import com.workmate.domain.conversation.MessageRole;
import com.workmate.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the core {@link AgentLoop}. Pure domain test — no Spring, no real LLM.
 * The LLM and tools are simple in-memory fakes so we can assert the loop's control flow:
 * tool execution, result feed-back, termination, and the iteration safeguard.
 */
class AgentLoopTest {

    private Conversation newConversationWithUserMessage() {
        Conversation conversation = Conversation.start(WorkspaceId.newId());
        conversation.addMessage(MessageRole.USER, MessageContent.of("이번 분기 환불 1위 제품은?"));
        return conversation;
    }

    /** An LlmClient that replays a fixed script of responses, recording each request. */
    private static final class ScriptedLlmClient implements LlmClient {
        private final Deque<LlmResponse> script;
        final AtomicInteger calls = new AtomicInteger();
        LlmRequest lastRequest;

        ScriptedLlmClient(LlmResponse... responses) {
            this.script = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public LlmResponse complete(LlmRequest request) {
            calls.incrementAndGet();
            this.lastRequest = request;
            return script.isEmpty() ? LlmResponse.ofText("fallback") : script.removeFirst();
        }
    }

    /** A single in-memory tool returning a canned result and counting executions. */
    private static final class FakeTool implements Tool {
        private final String name;
        private final ToolResult result;
        final AtomicInteger executions = new AtomicInteger();

        FakeTool(String name, ToolResult result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(name, "fake tool " + name, "{}");
        }

        @Override
        public ToolResult execute(String inputJson) {
            executions.incrementAndGet();
            return result;
        }
    }

    private ToolRegistry registryOf(Tool... tools) {
        Map<String, Tool> byName = new java.util.LinkedHashMap<>();
        for (Tool t : tools) {
            byName.put(t.name(), t);
        }
        return new ToolRegistry() {
            @Override
            public Optional<Tool> find(String name) {
                return Optional.ofNullable(byName.get(name));
            }

            @Override
            public List<Tool> all() {
                return List.copyOf(byName.values());
            }
        };
    }

    @Test
    void returns_final_answer_without_calling_any_tool() {
        ScriptedLlmClient llm = new ScriptedLlmClient(LlmResponse.ofText("환불 1위는 무선이어폰입니다."));
        Conversation conversation = newConversationWithUserMessage();

        AgentLoop loop = new AgentLoop(llm, registryOf(), MaxIterationsPolicy.defaultPolicy(), "system");
        AgentResult result = loop.run(conversation);

        assertThat(result.finalText()).isEqualTo("환불 1위는 무선이어폰입니다.");
        assertThat(result.iterations()).isEqualTo(1);
        assertThat(llm.calls.get()).isEqualTo(1);
        // user + assistant
        assertThat(conversation.messages()).hasSize(2);
        assertThat(conversation.messages().get(1).role()).isEqualTo(MessageRole.ASSISTANT);
    }

    @Test
    void executes_tool_then_feeds_result_back_and_answers() {
        FakeTool sql = new FakeTool("query_database", ToolResult.ok("{\"top\":\"무선이어폰\",\"count\":42}"));
        ScriptedLlmClient llm = new ScriptedLlmClient(
                LlmResponse.ofToolCalls(List.of(new ToolCall("query_database", "{\"q\":\"top refund\"}"))),
                LlmResponse.ofText("환불 1위는 무선이어폰(42건)입니다."));
        Conversation conversation = newConversationWithUserMessage();

        AgentLoop loop = new AgentLoop(llm, registryOf(sql), MaxIterationsPolicy.defaultPolicy(), "system");
        AgentResult result = loop.run(conversation);

        assertThat(sql.executions.get()).isEqualTo(1);
        assertThat(result.iterations()).isEqualTo(2);
        assertThat(result.finalText()).contains("무선이어폰");
        // user + tool(result) + assistant
        assertThat(conversation.messages()).hasSize(3);
        assertThat(conversation.messages().get(1).role()).isEqualTo(MessageRole.TOOL);
        assertThat(conversation.messages().get(1).content().value()).contains("query_database").contains("무선이어폰");
        // the tool result was fed back into the transcript sent on the 2nd LLM call
        assertThat(llm.lastRequest.messages()).anyMatch(m -> m.role().equals("TOOL"));
    }

    @Test
    void aborts_when_max_iterations_exceeded() {
        // Always asks for a tool, never answers -> would loop forever without the cap.
        FakeTool spinning = new FakeTool("noop", ToolResult.ok("{}"));
        LlmClient alwaysToolUse = request ->
                LlmResponse.ofToolCalls(List.of(new ToolCall("noop", "{}")));
        Conversation conversation = newConversationWithUserMessage();

        AgentLoop loop = new AgentLoop(alwaysToolUse, registryOf(spinning), new MaxIterationsPolicy(3), "system");

        assertThatThrownBy(() -> loop.run(conversation))
                .isInstanceOf(MaxIterationsExceededException.class)
                .hasMessageContaining("3");
    }

    @Test
    void fails_when_model_calls_unknown_tool() {
        LlmClient callsGhost = request ->
                LlmResponse.ofToolCalls(List.of(new ToolCall("ghost_tool", "{}")));
        Conversation conversation = newConversationWithUserMessage();

        AgentLoop loop = new AgentLoop(callsGhost, registryOf(), MaxIterationsPolicy.defaultPolicy(), "system");

        assertThatThrownBy(() -> loop.run(conversation))
                .isInstanceOf(ToolNotFoundException.class)
                .hasMessageContaining("ghost_tool");
    }
}
