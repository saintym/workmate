package com.workmate.domain.agent;

import com.workmate.domain.common.ValueObject;

import java.util.Collections;
import java.util.List;

/**
 * An immutable request to the LLM: the system prompt, the conversation transcript, and the
 * tools the model is allowed to call.
 *
 * <p>This is the domain-level, provider-agnostic request. The {@link LlmClient} adapter is
 * responsible for translating it into a concrete provider call (e.g. rendering tools and the
 * tool-use protocol into the prompt for the Claude CLI).
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param systemPrompt high-level instructions for the agent (role, behaviour); may be empty
 * @param messages     the conversation transcript, oldest first
 * @param tools        the tool definitions the model may invoke
 */
public record LlmRequest(String systemPrompt, List<LlmMessage> messages, List<ToolDefinition> tools)
        implements ValueObject {

    public LlmRequest {
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        messages = messages == null ? List.of() : Collections.unmodifiableList(List.copyOf(messages));
        tools = tools == null ? List.of() : Collections.unmodifiableList(List.copyOf(tools));
    }
}
