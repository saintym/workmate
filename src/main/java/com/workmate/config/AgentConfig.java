package com.workmate.config;

import com.workmate.domain.agent.LlmClient;
import com.workmate.domain.agent.MaxIterationsPolicy;
import com.workmate.domain.agent.ToolRegistry;
import com.workmate.domain.agent.service.AgentLoop;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the agentic loop.
 *
 * <p>Wires together the {@link AgentLoop} domain service with its required ports.
 * The {@link LlmClient} and {@link ToolRegistry} implementations are provided by the
 * infrastructure layer — selected at runtime via the {@code app.llm.provider} property
 * (e.g. {@code claude-cli} for the real Claude CLI adapter, {@code mock} for tests and
 * local development without an LLM binary).
 */
@Configuration
public class AgentConfig {

    /**
     * Concise bilingual system prompt (Korean primary) describing the agent's role
     * and available tools to the underlying LLM.
     */
    private static final String SYSTEM_PROMPT =
            """
            당신은 Workmate입니다 — 사내 직원을 돕는 AI 어시스턴트입니다.
            사용 가능한 도구를 활용해 직원의 질문에 답하세요.
            매출·주문·환불·고객 등 정형 비즈니스 데이터에 관한 질문에는 query_database 도구를 사용하세요.
            도구가 데이터를 반환하면 사용자가 이해하기 쉽게 요약해 주세요.
            답변은 사용자가 사용한 언어로, 간결하게 작성하세요.
            You are Workmate, an in-house AI assistant. Use the available tools to answer employee questions.
            For questions about structured business data (products, orders, refunds, customers), use the query_database tool.
            If a tool returns data, summarize it clearly for the user.
            Answer in the user's language, concisely.
            """;

    /**
     * Creates the {@link AgentLoop} bean.
     *
     * @param llmClient    the LLM port — implementation selected by {@code app.llm.provider}
     * @param toolRegistry the tool registry port — populated by infrastructure tool beans
     * @return a configured {@link AgentLoop} using the default iteration cap
     */
    @Bean
    public AgentLoop agentLoop(LlmClient llmClient, ToolRegistry toolRegistry) {
        return new AgentLoop(llmClient, toolRegistry, MaxIterationsPolicy.defaultPolicy(), SYSTEM_PROMPT);
    }
}
