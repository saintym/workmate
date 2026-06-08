package com.workmate.domain.agent;

/**
 * Port for invoking a large language model.
 *
 * <p>Given a provider-agnostic {@link LlmRequest} (transcript + tools), it returns an
 * {@link LlmResponse} that is either a final text answer or a tool-use request. The adapter
 * owns the wire protocol: how tools are advertised in the prompt and how a tool-use reply is
 * parsed back out. Swapping Claude CLI for the Anthropic API (or a mock) requires no change
 * to the domain.
 *
 * <p>Pure Java — no framework dependencies.
 */
public interface LlmClient {

    /**
     * Complete one step of the conversation.
     *
     * @param request the transcript, system prompt, and available tools
     * @return the model's reply (final text or tool calls); never {@code null}
     */
    LlmResponse complete(LlmRequest request);
}
