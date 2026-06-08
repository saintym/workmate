package com.workmate.config;

import com.workmate.domain.knowledge.ChunkingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for knowledge/RAG domain services.
 *
 * <p>{@link ChunkingService} is a pure-Java domain class with no Spring dependencies,
 * so it is registered here as a {@link Bean} to allow constructor injection elsewhere
 * (e.g. {@link com.workmate.application.document.DocumentIndexingService}).
 */
@Configuration
public class KnowledgeConfig {

    /**
     * Registers the {@link ChunkingService} domain service as a Spring-managed bean.
     *
     * @return a new {@link ChunkingService} instance
     */
    @Bean
    public ChunkingService chunkingService() {
        return new ChunkingService();
    }
}
