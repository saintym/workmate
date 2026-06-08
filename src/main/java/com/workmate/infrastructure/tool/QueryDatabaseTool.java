package com.workmate.infrastructure.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workmate.domain.agent.Tool;
import com.workmate.domain.agent.ToolDefinition;
import com.workmate.domain.agent.ToolInput;
import com.workmate.domain.agent.ToolResult;
import com.workmate.domain.database.QueryExecutor;
import com.workmate.domain.database.QueryResult;
import com.workmate.domain.database.QuerySafetyPolicy;
import com.workmate.domain.database.SqlQuery;
import com.workmate.domain.database.SqlValidator;
import com.workmate.domain.database.TableWhitelist;
import com.workmate.domain.database.UnsafeQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent tool that executes a read-only SQL SELECT against the business database.
 *
 * <p>The tool validates and hardens the incoming query via {@link SqlValidator} before
 * delegating to {@link QueryExecutor}. Any {@link UnsafeQueryException} or unexpected
 * error is returned as a {@link ToolResult#error} — this method never throws.
 */
@Component
public class QueryDatabaseTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(QueryDatabaseTool.class);

    private static final TableWhitelist WHITELIST =
            TableWhitelist.of("products", "orders", "refunds", "customers");
    private static final QuerySafetyPolicy POLICY =
            QuerySafetyPolicy.defaultPolicy(WHITELIST);

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "query_database",
            "Run a read-only SQL SELECT against the business database (products, orders, " +
            "refunds, customers) to answer questions about structured data. " +
            "비즈니스 데이터베이스(products, orders, refunds, customers)에 대해 읽기 전용 SQL SELECT를 실행하여 " +
            "정형 데이터 관련 질문에 답합니다.",
            """
            {
              "type": "object",
              "required": ["sql"],
              "properties": {
                "sql": {
                  "type": "string",
                  "description": "A read-only SQL SELECT statement to execute against the database."
                }
              },
              "additionalProperties": false
            }
            """
    );

    private final SqlValidator sqlValidator;
    private final QueryExecutor queryExecutor;
    private final ObjectMapper objectMapper;

    public QueryDatabaseTool(SqlValidator sqlValidator,
                              QueryExecutor queryExecutor,
                              ObjectMapper objectMapper) {
        this.sqlValidator = sqlValidator;
        this.queryExecutor = queryExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolResult execute(ToolInput toolInput) {
        try {
            // workspaceId is available via toolInput.workspaceId() for tenant scoping;
            // the business tables are shared in this demo, so SELECT/whitelist/LIMIT remain
            // the safety boundary. TODO: inject workspace_id predicates into the SQL.
            JsonNode input = objectMapper.readTree(toolInput.json());
            JsonNode sqlNode = input.get("sql");
            if (sqlNode == null || sqlNode.isNull() || sqlNode.asText().isBlank()) {
                return errorResult("Missing required parameter: sql");
            }
            String rawSql = sqlNode.asText();

            // Validate and harden
            SqlQuery safeQuery = sqlValidator.validateAndHarden(new SqlQuery(rawSql), POLICY);

            // Execute
            QueryResult result = queryExecutor.execute(safeQuery);

            // Serialise result to JSON
            String resultJson = serializeResult(result);
            return ToolResult.ok(resultJson);

        } catch (UnsafeQueryException e) {
            log.warn("query_database rejected unsafe query: {}", e.getMessage());
            return errorResult(e.getMessage());
        } catch (Exception e) {
            log.error("query_database unexpected error", e);
            return errorResult("Unexpected error: " + e.getMessage());
        }
    }

    private String serializeResult(QueryResult result) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("rowCount", result.rowCount());
        root.set("columns", objectMapper.valueToTree(result.columns()));

        var rowsArray = objectMapper.createArrayNode();
        for (var row : result.rows()) {
            rowsArray.add(objectMapper.valueToTree(row));
        }
        root.set("rows", rowsArray);
        return objectMapper.writeValueAsString(root);
    }

    private ToolResult errorResult(String message) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("error", message);
            return ToolResult.error(objectMapper.writeValueAsString(node));
        } catch (JsonProcessingException e) {
            // Fallback to raw JSON — should never happen
            return ToolResult.error("{\"error\":\"" + message.replace("\"", "'") + "\"}");
        }
    }
}
