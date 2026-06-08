package com.workmate.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.domain.agent.LlmClient;
import com.workmate.domain.agent.LlmMessage;
import com.workmate.domain.agent.LlmRequest;
import com.workmate.domain.agent.LlmResponse;
import com.workmate.domain.agent.ToolCall;
import com.workmate.domain.agent.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link LlmClient} adapter that invokes the local Claude CLI process headless.
 *
 * <p>Activated when {@code app.llm.provider=claude-cli} (the default).
 * The full conversation + tool protocol is rendered into a single prompt string
 * passed via {@code -p}, and the CLI's JSON output is parsed back into an
 * {@link LlmResponse}.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "claude-cli", matchIfMissing = true)
public class ClaudeCliLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCliLlmClient.class);

    private final ObjectMapper objectMapper;
    private final String command;
    private final String model;
    private final int timeoutSeconds;

    public ClaudeCliLlmClient(
            ObjectMapper objectMapper,
            @Value("${app.llm.claude-cli.command:claude}") String command,
            @Value("${app.llm.claude-cli.model:}") String model,
            @Value("${app.llm.claude-cli.timeout-seconds:60}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.command = command;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String prompt = buildPrompt(request);

        List<String> args = new ArrayList<>();
        args.add(command);
        args.add("-p");
        args.add(prompt);
        args.add("--output-format");
        args.add("json");
        args.add("--allowedTools");
        args.add("");
        if (model != null && !model.isBlank()) {
            args.add("--model");
            args.add(model);
        }

        log.debug("Invoking Claude CLI: command={}, model={}", command, model);

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            // `claude -p` reads stdin and otherwise waits ~3s emitting a "no stdin data"
            // warning. Redirect stdin from /dev/null so it returns immediately.
            pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
            // Keep stderr OUT of stdout: merging them lets CLI warnings corrupt the JSON
            // envelope we parse. Discard stderr (the JSON envelope already reports errors).
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            process = pb.start();

            byte[] outputBytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Claude CLI timed out after {}s", timeoutSeconds);
                return LlmResponse.ofText("(오류) LLM 응답 시간이 초과되었습니다.");
            }

            int exitCode = process.exitValue();
            String stdout = new String(outputBytes, StandardCharsets.UTF_8).trim();

            log.debug("Claude CLI exited with code={}, stdout length={}", exitCode, stdout.length());

            if (exitCode != 0 || stdout.isEmpty()) {
                log.warn("Claude CLI returned non-zero exit={} or empty output", exitCode);
                return LlmResponse.ofText("(오류) LLM 호출에 실패했습니다. (exit=" + exitCode + ")");
            }

            return parseCliOutput(stdout);

        } catch (IOException e) {
            log.error("Failed to start Claude CLI process", e);
            return LlmResponse.ofText("(오류) Claude CLI 프로세스를 시작할 수 없습니다: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for Claude CLI", e);
            return LlmResponse.ofText("(오류) LLM 호출이 중단되었습니다.");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Prompt building
    // -------------------------------------------------------------------------

    private String buildPrompt(LlmRequest request) {
        StringBuilder sb = new StringBuilder();

        // System prompt (agent role)
        if (!request.systemPrompt().isBlank()) {
            sb.append(request.systemPrompt()).append("\n\n");
        }

        // Tool protocol block
        if (!request.tools().isEmpty()) {
            sb.append("=== 도구 프로토콜 / TOOL PROTOCOL ===\n");
            sb.append("사용 가능한 도구 목록 / Available tools:\n");
            for (ToolDefinition tool : request.tools()) {
                sb.append("- ").append(tool.name()).append(": ").append(tool.description())
                        .append(" (input schema: ").append(tool.inputSchemaJson()).append(")\n");
            }
            sb.append("\n");
            sb.append("도구를 호출해야 할 경우, 아래의 미니파이된 JSON 한 줄만 출력하세요 (다른 텍스트 없이):\n");
            sb.append("If you need to call a tool, reply with ONLY a single line of minified JSON (nothing else):\n");
            sb.append("{\"tool_calls\":[{\"name\":\"<tool>\",\"input\":{...}}]}\n");
            sb.append("\n");
            sb.append("직접 답변할 수 있다면 일반 자연어로 답하세요 (JSON 없이).\n");
            sb.append("If you can answer directly, reply with a normal natural-language answer (no JSON).\n");
            sb.append("=== END OF TOOL PROTOCOL ===\n\n");
        }

        // Conversation transcript
        for (LlmMessage message : request.messages()) {
            sb.append(message.role()).append(": ").append(message.content()).append("\n");
        }

        // Prime the completion
        sb.append("ASSISTANT:");

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    private LlmResponse parseCliOutput(String stdout) {
        // Parse the outer CLI JSON envelope: {"type":"result","result":"<assistant text>",...}
        String replyText;
        try {
            JsonNode root = objectMapper.readTree(stdout);
            JsonNode resultNode = root.get("result");
            if (resultNode == null || resultNode.isNull()) {
                log.warn("Claude CLI JSON has no 'result' field; treating as plain text");
                replyText = stdout;
            } else {
                replyText = resultNode.asText().trim();
            }
        } catch (Exception e) {
            log.debug("CLI stdout is not JSON envelope, using raw text", e);
            replyText = stdout;
        }

        // Try to interpret the assistant reply as a tool-call JSON.
        // Strip a ```json ... ``` code fence first in case the model wrapped it.
        String trimmed = stripCodeFence(replyText.trim());
        if (trimmed.startsWith("{") && trimmed.contains("tool_calls")) {
            try {
                JsonNode replyNode = objectMapper.readTree(trimmed);
                JsonNode toolCallsNode = replyNode.get("tool_calls");
                if (toolCallsNode != null && toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                    List<ToolCall> toolCalls = new ArrayList<>();
                    for (JsonNode tc : toolCallsNode) {
                        String name = tc.get("name").asText();
                        JsonNode inputNode = tc.get("input");
                        String inputJson = inputNode != null
                                ? objectMapper.writeValueAsString(inputNode)
                                : "{}";
                        toolCalls.add(new ToolCall(name, inputJson));
                    }
                    log.info("Claude CLI returned tool-use: tools={}", toolCalls.stream()
                            .map(ToolCall::toolName).toList());
                    return LlmResponse.ofToolCalls(toolCalls);
                }
            } catch (Exception e) {
                log.debug("Reply looked like tool JSON but parse failed; treating as text", e);
            }
        }

        log.info("Claude CLI returned text response (length={})", trimmed.length());
        return LlmResponse.ofText(trimmed);
    }

    /** Remove a surrounding ```/```json fenced block if the model wrapped its reply. */
    private String stripCodeFence(String s) {
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.trim();
    }
}
