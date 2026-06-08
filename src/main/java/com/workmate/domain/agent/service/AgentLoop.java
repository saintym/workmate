package com.workmate.domain.agent.service;

import com.workmate.domain.agent.AgentResult;
import com.workmate.domain.agent.AgentState;
import com.workmate.domain.agent.LlmClient;
import com.workmate.domain.agent.LlmMessage;
import com.workmate.domain.agent.LlmRequest;
import com.workmate.domain.agent.LlmResponse;
import com.workmate.domain.agent.MaxIterationsPolicy;
import com.workmate.domain.agent.Tool;
import com.workmate.domain.agent.ToolCall;
import com.workmate.domain.agent.ToolInput;
import com.workmate.domain.agent.ToolNotFoundException;
import com.workmate.domain.agent.ToolRegistry;
import com.workmate.domain.agent.ToolResult;
import com.workmate.domain.conversation.Conversation;
import com.workmate.domain.conversation.Message;
import com.workmate.domain.conversation.MessageContent;
import com.workmate.domain.conversation.MessageRole;

import java.util.List;
import java.util.Objects;

/**
 * The core agentic loop — the heart of Workmate.
 *
 * <p>Given a {@link Conversation} whose latest message is the user's question, it repeatedly:
 * <ol>
 *   <li>sends the transcript + available tool definitions to the {@link LlmClient};</li>
 *   <li>if the model asks to use tools, executes each via the {@link ToolRegistry} and
 *       <b>feeds the results back</b> into the conversation as {@code TOOL} messages;</li>
 *   <li>repeats until the model returns a final text answer.</li>
 * </ol>
 *
 * <p>Termination is guaranteed by {@link MaxIterationsPolicy}: every LLM call advances the
 * {@link AgentState} counter, and exceeding the cap aborts with
 * {@link com.workmate.domain.agent.MaxIterationsExceededException} — the safeguard against
 * infinite tool-calling loops.
 *
 * <p>This class is deliberately framework-free pure-Java domain logic. The LLM wire protocol
 * (how tools are advertised, how tool-use is parsed) lives behind {@link LlmClient}; tool
 * execution lives behind {@link Tool}. Both are ports, so the loop is fully unit-testable
 * with a mock LLM and in-memory tools.
 */
public final class AgentLoop {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final MaxIterationsPolicy maxIterations;
    private final String systemPrompt;

    public AgentLoop(LlmClient llmClient,
                     ToolRegistry toolRegistry,
                     MaxIterationsPolicy maxIterations,
                     String systemPrompt) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.maxIterations = Objects.requireNonNull(maxIterations, "maxIterations must not be null");
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
    }

    /**
     * Run the loop against the given conversation until the model produces a final answer.
     *
     * <p>Mutates {@code conversation}: appends {@code TOOL} messages for every tool result and
     * a final {@code ASSISTANT} message for the answer (each raising a domain event the
     * application layer publishes after persisting).
     *
     * @param conversation the conversation whose latest message is the user's question
     * @return the final answer and the iteration count
     * @throws com.workmate.domain.agent.MaxIterationsExceededException if the cap is reached
     * @throws ToolNotFoundException                                    if the model calls an unknown tool
     */
    public AgentResult run(Conversation conversation) {
        Objects.requireNonNull(conversation, "conversation must not be null");
        AgentState state = AgentState.start(maxIterations);

        while (true) {
            state.beginIteration(); // throws once the cap is exceeded — guarantees termination

            LlmResponse response = llmClient.complete(buildRequest(conversation));

            if (!response.isToolUse()) {
                conversation.addMessage(MessageRole.ASSISTANT, MessageContent.of(answerOrFallback(response)));
                return new AgentResult(answerOrFallback(response), state.iterations());
            }

            // Execute every requested tool and feed each result back into the transcript,
            // so the next iteration lets the model reason over what the tools returned.
            for (ToolCall call : response.toolCalls()) {
                Tool tool = toolRegistry.find(call.toolName())
                        .orElseThrow(() -> new ToolNotFoundException(call.toolName()));
                // Tenant context comes from the conversation, never from the LLM — so
                // tenant-scoped tools cannot be tricked into crossing workspaces.
                ToolResult result = tool.execute(new ToolInput(conversation.workspaceId(), call.inputJson()));
                conversation.addMessage(MessageRole.TOOL, MessageContent.of(renderToolResult(call, result)));
            }
        }
    }

    private LlmRequest buildRequest(Conversation conversation) {
        List<LlmMessage> transcript = conversation.messages().stream()
                .map(this::toLlmMessage)
                .toList();
        return new LlmRequest(systemPrompt, transcript, toolRegistry.definitions());
    }

    private LlmMessage toLlmMessage(Message message) {
        return new LlmMessage(message.role().name(), message.content().value());
    }

    /** Render a tool result as a transcript line the model can read on the next turn. */
    private String renderToolResult(ToolCall call, ToolResult result) {
        String status = result.success() ? "OK" : "ERROR";
        return "[tool:" + call.toolName() + "][" + status + "] " + result.outputJson();
    }

    private String answerOrFallback(LlmResponse response) {
        return response.text().isBlank()
                ? "죄송합니다. 답변을 생성하지 못했습니다."
                : response.text();
    }
}
