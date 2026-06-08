package com.workmate.infrastructure.llm;

import com.workmate.domain.knowledge.Embedding;
import com.workmate.domain.knowledge.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * {@link EmbeddingService} adapter that delegates to OpenAI's
 * {@code text-embedding-3-small} model (1536 dimensions).
 *
 * <p>Activated when {@code app.embedding.provider=openai}.  Requires
 * {@code app.openai.api-key} to be set; a missing or blank key causes an
 * {@link IllegalStateException} at embed-time (not at startup) so the
 * application can boot without the key present.
 *
 * <p>Uses Spring WebClient (reactor-netty on classpath via spring-boot-starter-webflux)
 * with a 3-attempt exponential-backoff retry (initial delay 500 ms) and a 30-second
 * per-call timeout.
 */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private static final int DIMENSION = 1536;
    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final String apiKey;

    public OpenAiEmbeddingService(
            WebClient.Builder webClientBuilder,
            @Value("${app.openai.api-key:}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(EMBEDDINGS_URL).build();
        this.apiKey = apiKey;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    public Embedding embed(String text) {
        requireApiKey();

        log.debug("OpenAiEmbeddingService: requesting embedding for text of length {}", text.length());

        EmbeddingResponse response = webClient.post()
                .uri("")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("model", MODEL, "input", text))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> log.warn(
                                "OpenAI embedding request retry attempt {}: {}",
                                signal.totalRetries() + 1, signal.failure().getMessage())))
                .block(TIMEOUT);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("OpenAI returned an empty embedding response");
        }

        float[] vector = toFloatArray(response.data().get(0).embedding());
        log.debug("OpenAiEmbeddingService: received embedding with {} dimensions", vector.length);
        return new Embedding(vector);
    }

    @Override
    public List<Embedding> embedAll(List<String> texts) {
        requireApiKey();

        log.debug("OpenAiEmbeddingService: batch embedding {} texts", texts.size());

        EmbeddingResponse response = webClient.post()
                .uri("")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("model", MODEL, "input", texts))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> log.warn(
                                "OpenAI batch embedding retry attempt {}: {}",
                                signal.totalRetries() + 1, signal.failure().getMessage())))
                .block(TIMEOUT);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("OpenAI returned an empty batch embedding response");
        }

        return response.data().stream()
                .map(d -> new Embedding(toFloatArray(d.embedding())))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set app.openai.api-key (or OPENAI_API_KEY) " +
                    "or switch to the default hashing embedder via app.embedding.provider=hashing.");
        }
    }

    private static float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            arr[i] = doubles.get(i).floatValue();
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // Response DTOs (package-private for testability)
    // -------------------------------------------------------------------------

    record EmbeddingResponse(List<EmbeddingData> data) {}

    record EmbeddingData(List<Double> embedding, int index) {}
}
