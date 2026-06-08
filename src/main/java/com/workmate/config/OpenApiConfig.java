package com.workmate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 configuration.
 *
 * <p>Exposes a customised {@link OpenAPI} bean that sets the API title and version.
 * The Swagger UI is available at the path configured via {@code springdoc.swagger-ui.path}
 * in {@code application.yml} (default: /swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workmateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workmate API")
                        .version("0.1.0")
                        .description("Multi-tenant agentic AI platform: RAG + Text-to-SQL + SSE chat"));
    }
}
