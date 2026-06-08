package com.workmate.infrastructure.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workmate.domain.agent.Tool;
import com.workmate.domain.agent.ToolDefinition;
import com.workmate.domain.agent.ToolInput;
import com.workmate.domain.agent.ToolResult;
import com.workmate.domain.knowledge.ChunkMatch;
import com.workmate.domain.knowledge.Embedding;
import com.workmate.domain.knowledge.EmbeddingService;
import com.workmate.domain.knowledge.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Agent tool that searches uploaded workspace documents by semantic similarity.
 *
 * <p>Embeds the incoming query, performs a top-3 approximate nearest-neighbour search
 * within the caller's workspace, and returns the matching chunks as JSON. The
 * {@code workspaceId} is taken from {@link ToolInput} — never from the LLM — so
 * tenant isolation is guaranteed.
 *
 * <p>This method never throws: any error is caught and returned as
 * {@link ToolResult#error}.
 */
@Component
public class SearchDocumentsTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(SearchDocumentsTool.class);
    private static final int TOP_K = 3;

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "search_documents",
            "Search the company's uploaded documents (policies, guides) by meaning to answer "
                    + "questions about unstructured knowledge. "
                    + "사내 업로드 문서를 의미 기반으로 검색합니다.",
            """
            {
              "type": "object",
              "required": ["query"],
              "properties": {
                "query": {
                  "type": "string",
                  "description": "The natural-language search query to find relevant document chunks."
                }
              },
              "additionalProperties": false
            }
            """
    );

    private final EmbeddingService embeddingService;
    private final VectorSearchRepository vectorSearchRepository;
    private final ObjectMapper objectMapper;

    public SearchDocumentsTool(EmbeddingService embeddingService,
                                VectorSearchRepository vectorSearchRepository,
                                ObjectMapper objectMapper) {
        this.embeddingService = Objects.requireNonNull(embeddingService,
                "embeddingService must not be null");
        this.vectorSearchRepository = Objects.requireNonNull(vectorSearchRepository,
                "vectorSearchRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper,
                "objectMapper must not be null");
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    /**
     * Execute a semantic document search for the given query within the caller's workspace.
     *
     * @param input the tool arguments and tenant context; workspaceId enforces isolation
     * @return a {@link ToolResult#ok} with matched chunks JSON, or {@link ToolResult#error}
     *         on any failure
     */
    @Override
    public ToolResult execute(ToolInput input) {
        try {
            JsonNode root = objectMapper.readTree(input.json());
            JsonNode queryNode = root.get("query");
            if (queryNode == null || queryNode.isNull() || queryNode.asText().isBlank()) {
                return errorResult("Missing required parameter: query");
            }
            String query = queryNode.asText().trim();

            log.info("search_documents: workspaceId={}, query={}", input.workspaceId(), query);

            Embedding queryEmbedding = embeddingService.embed(query);
            List<ChunkMatch> matches = vectorSearchRepository.findSimilar(
                    input.workspaceId(), queryEmbedding, TOP_K);

            String resultJson = serializeMatches(matches);
            return ToolResult.ok(resultJson);

        } catch (Exception e) {
            log.error("search_documents: unexpected error for workspaceId={}", input.workspaceId(), e);
            return errorResult("Unexpected error: " + e.getMessage());
        }
    }

    // --- private helpers ---

    private String serializeMatches(List<ChunkMatch> matches) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("resultCount", matches.size());
        ArrayNode results = objectMapper.createArrayNode();
        for (ChunkMatch match : matches) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("documentId", match.documentId().value().toString());
            node.put("chunkIndex", match.chunkIndex());
            node.put("content", match.content());
            node.put("score", match.score().value());
            results.add(node);
        }
        root.set("results", results);
        return objectMapper.writeValueAsString(root);
    }

    private ToolResult errorResult(String message) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("error", message);
            return ToolResult.error(objectMapper.writeValueAsString(node));
        } catch (JsonProcessingException e) {
            return ToolResult.error("{\"error\":\"" + message.replace("\"", "'") + "\"}");
        }
    }
}
