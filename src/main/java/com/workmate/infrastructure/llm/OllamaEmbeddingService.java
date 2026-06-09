package com.workmate.infrastructure.llm;

import com.workmate.domain.knowledge.Embedding;
import com.workmate.domain.knowledge.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Real local semantic embeddings via a running <a href="https://ollama.com">Ollama</a> server
 * (model {@code nomic-embed-text}, 768-dim). Unlike {@link HashingEmbeddingService} — which
 * only approximates similarity by word overlap — this produces genuine meaning-based vectors,
 * so synonyms ("연차" vs "쉬는 날") land near each other.
 *
 * <p>Cost-free and offline: it calls the local Ollama HTTP API, not a paid cloud API.
 *
 * <p>Activated when {@code app.embedding.provider=ollama}. The pgvector column must match the
 * model dimension (768) — see {@code db/init/03-vector-ollama.sql}.
 */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "ollama")
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    /** nomic-embed-text output size. */
    private static final int DIMENSION = 768;

    private final WebClient webClient;
    private final String model;

    public OllamaEmbeddingService(
            @Value("${app.embedding.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.embedding.ollama.model:nomic-embed-text}") String model) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.model = model;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Embedding embed(String text) {
        String input = (text == null || text.isBlank()) ? " " : text;

        Map<String, Object> response = webClient.post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("model", model, "input", input))
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
                .block(Duration.ofSeconds(30));

        if (response == null || !response.containsKey("embeddings")) {
            throw new IllegalStateException("Ollama returned no embeddings (model=" + model + ")");
        }

        // /api/embed returns {"embeddings": [[...768 floats...]]}
        List<List<Number>> embeddings = (List<List<Number>>) response.get("embeddings");
        if (embeddings.isEmpty() || embeddings.get(0).isEmpty()) {
            throw new IllegalStateException("Ollama returned an empty embedding (model=" + model + ")");
        }

        List<Number> first = embeddings.get(0);
        float[] vec = new float[first.size()];
        for (int i = 0; i < first.size(); i++) {
            vec[i] = first.get(i).floatValue();
        }
        log.trace("OllamaEmbeddingService: embedded text len={} into dim={}", input.length(), vec.length);
        return new Embedding(vec);
    }

    @Override
    public List<Embedding> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
