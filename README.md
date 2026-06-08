# Workmate

A multi-tenant agentic AI platform that lets employees ask questions in natural language and
receive answers drawn from internal documents (**RAG**) and internal databases (**Text-to-SQL**),
streamed back token-by-token over **Server-Sent Events (SSE)**.

A natural-language question is answered by an **agent loop** that decides, on its own, whether
to query the business database (Text-to-SQL) or search internal documents (RAG), executes the
chosen tools, feeds the results back to the model, and streams the final answer.

---

## Architecture

Workmate follows **Hexagonal Architecture** (Ports & Adapters) combined with **Domain-Driven
Design** and **CQRS**.

```
┌─────────────────────────────────────────────────────────┐
│  interfaces/rest          REST controllers + DTOs        │
│  ─────────────────────────────────────────────────────  │
│  application              Command / Query handlers       │
│  ─────────────────────────────────────────────────────  │
│  domain                   Pure-Java aggregates, VOs,     │
│                           domain events, repo interfaces  │
│  ─────────────────────────────────────────────────────  │
│  infrastructure/jpa       JPA entities, adapters         │
│  infrastructure/messaging Kafka event publisher adapter  │
│  infrastructure/llm       LlmClient (Claude CLI / mock)  │
│  infrastructure/database  Text-to-SQL validate + execute │
│  infrastructure/tool      Tool registry + QueryDatabase  │
│  infrastructure/storage   Document storage (S3 — Phase 3)│
│  ─────────────────────────────────────────────────────  │
│  config                   Spring @Configuration classes  │
└─────────────────────────────────────────────────────────┘
```

### Bounded Contexts

| Context        | Responsibility                                                     |
|----------------|--------------------------------------------------------------------|
| `workspace`    | Multi-tenant isolation — each company is a workspace              |
| `conversation` | Chat sessions, message history, agent turn management             |
| `knowledge`    | Document upload, chunking, pgvector embeddings (RAG)              |
| `agent`        | Agent Loop: tool selection, LLM orchestration, Tool Use           |
| `database`     | Text-to-SQL: natural-language → validated SQL → read-only execute |

---

## Tech Stack

| Layer           | Technology                                      |
|-----------------|-------------------------------------------------|
| Language        | Java 17                                         |
| Build           | Gradle 8.7 (wrapper)                             |
| Framework       | Spring Boot 3.2, Spring WebFlux (reactive SSE)  |
| Persistence     | Spring Data JPA, PostgreSQL 16 + pgvector       |
| Messaging       | Apache Kafka (Spring Kafka)                     |
| Text-to-SQL     | JSqlParser (SELECT-only / whitelist / LIMIT)    |
| Observability   | Spring Actuator, Micrometer, Prometheus         |
| API docs        | SpringDoc OpenAPI 3 (Swagger UI)                |
| LLM             | Pluggable `LlmClient` — Claude CLI (default, cost-free) or mock; Anthropic API / OpenAI embeddings in later phases |

---

## Getting Started

### Prerequisites

- JDK 17+
- Docker (for PostgreSQL + Kafka)
- The local **Claude CLI** on `PATH` (default LLM backend — avoids paid API cost). Set
  `LLM_PROVIDER=mock` to run fully offline with a deterministic stub instead.

### 1. Start infrastructure

```bash
docker compose up -d          # PostgreSQL (pgvector) + Kafka (KRaft)
```

`db/init/01-init.sql` enables the `vector` extension on first init. Load the demo
business data (products / orders / refunds / customers) for Text-to-SQL:

```bash
bash db/apply-business.sh     # applies db/init/02-business.sql to the running container
bash db/apply-vector.sh       # creates the pgvector document_chunks table + IVFFlat index
```

### 2. Build

```bash
./gradlew build
```

### 3. Run

```bash
# first run creates the schema; LLM defaults to the Claude CLI
SPRING_JPA_HIBERNATE_DDL_AUTO=update ./gradlew bootRun
```

The server starts on **http://localhost:8080**. Try it:

```bash
curl -N -X POST localhost:8080/api/v1/chat/messages \
  -H 'Content-Type: application/json' -H "X-Workspace-Id: $(uuidgen)" \
  -d '{"content":"환불이 가장 많았던 제품 top 3를 알려줘"}'
```

### Environment variables (optional overrides)

| Variable                  | Default                              | Description                  |
|---------------------------|--------------------------------------|------------------------------|
| `DATASOURCE_URL`          | `jdbc:postgresql://localhost:5432/workmate` | JDBC URL             |
| `DATASOURCE_USERNAME`     | `workmate`                           | DB username                  |
| `DATASOURCE_PASSWORD`     | `workmate`                           | DB password                  |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                     | Kafka broker address         |
| `LLM_PROVIDER`            | `claude-cli`                         | LLM backend: `claude-cli` or `mock` |
| `CLAUDE_CLI_COMMAND`      | `claude`                             | Claude CLI executable        |
| `CLAUDE_CLI_MODEL`        | _(empty)_                            | Optional model override      |
| `CLAUDE_CLI_TIMEOUT`      | `60`                                 | Per-call timeout (seconds)   |
| `EMBEDDING_PROVIDER`      | `hashing`                            | Embedding backend: `hashing` or `openai` |
| `OPENAI_API_KEY`          | _(empty)_                            | OpenAI API key (embeddings when `EMBEDDING_PROVIDER=openai`) |
| `S3_BUCKET`               | `workmate-docs`                      | S3 bucket for documents      |

---

## API Endpoints

All requests require the `X-Workspace-Id: <UUID>` header for tenant isolation.

| Method | Path                          | Description                                          | Response            |
|--------|-------------------------------|------------------------------------------------------|---------------------|
| `POST` | `/api/v1/chat/messages`       | Send a message; streams assistant reply as SSE       | `text/event-stream` |
| `GET`  | `/api/v1/conversations/{id}`  | Retrieve a conversation with full message history    | `200 OK`            |
| `POST` | `/api/v1/documents`           | Upload document metadata (async RAG indexing)        | `202 Accepted`      |
| `GET`  | `/api/v1/documents/{id}`      | Retrieve document metadata and indexing status       | `200 OK`            |
| `GET`  | `/actuator/health`            | Health check                                         | `200 OK`            |
| `GET`  | `/actuator/prometheus`        | Prometheus metrics scrape endpoint                   | `200 OK`            |
| `GET`  | `/swagger-ui.html`            | Interactive API documentation (Swagger UI)           | HTML                |

### SSE stream format (POST /api/v1/chat/messages)

```
event: message
data: Hello

event: message
data: world

event: done
data: <conversationId UUID>
```

---

## Project Layout

```
workmate/
├── src/main/java/com/workmate/
│   ├── WorkmateApplication.java          # @SpringBootApplication entry point
│   ├── config/
│   │   ├── JpaConfig.java                # JPA scanning + auditing
│   │   ├── KafkaConfig.java              # domain.events topic declaration
│   │   └── OpenApiConfig.java            # SpringDoc OpenAPI bean
│   ├── domain/
│   │   ├── common/                       # AggregateRoot, DomainEvent, DomainException …
│   │   ├── workspace/                    # Workspace aggregate + repository port
│   │   ├── conversation/                 # Conversation aggregate, Message, MessageRole …
│   │   ├── knowledge/                    # Document, DocumentChunk, Embedding …
│   │   ├── agent/                        # Tool, LlmClient, ToolRegistry ports + AgentLoop
│   │   └── database/                     # SqlQuery, QuerySafetyPolicy, SqlValidator port …
│   ├── application/
│   │   ├── chat/                         # ChatService (runs the agent loop), commands/queries
│   │   └── document/                     # DocumentUploadService, UploadDocumentCommand …
│   ├── infrastructure/
│   │   ├── jpa/                          # JPA entities, repository adapters
│   │   ├── messaging/                    # KafkaEventPublisher
│   │   ├── llm/                          # ClaudeCliLlmClient, MockLlmClient, embedding adapters
│   │   ├── database/                     # JSqlParserSqlValidator, JdbcQueryExecutor
│   │   ├── persistence/pgvector/         # VectorSearchRepositoryAdapter (cosine search)
│   │   ├── tool/                         # SpringToolRegistry, QueryDatabaseTool, SearchDocumentsTool
│   │   └── storage/                      # LocalDocumentStorage (document text)
│   └── interfaces/rest/
│       ├── ChatController.java
│       ├── DocumentController.java
│       ├── RestExceptionHandler.java
│       └── dto/                          # Request / Response records
├── src/test/java/…                       # AgentLoopTest, JSqlParserSqlValidatorTest
├── db/init/                              # 01-init.sql (pgvector), 02-business.sql (seed)
├── docker-compose.yml                    # Postgres + pgvector, Kafka (KRaft)
├── build.gradle
└── README.md
```

---

## Roadmap

### Phase 1 — DDD skeleton (complete)
Hexagonal package structure, domain aggregates (Workspace, Conversation, Document),
application services with CQRS (ChatService, DocumentUploadService), REST controllers
with SSE, JPA adapters, Kafka domain-event publishing, configuration, and this documentation.

### Phase 2 — Text-to-SQL + Agent Loop (complete)
- **Agent Loop** (`domain/agent/service/AgentLoop.java`) — pure-Java loop: send transcript +
  tool definitions to the LLM, execute requested tools, feed results back, repeat until a
  final answer; bounded by `MaxIterationsPolicy` to prevent infinite tool-calling. Unit-tested.
- **`LlmClient` port** with two adapters: `ClaudeCliLlmClient` (drives the local Claude CLI
  headless — no API cost; the tool-use protocol is prompt-based and parsed by the adapter) and
  `MockLlmClient` (deterministic, offline).
- **`database` context** — Text-to-SQL safety: `JSqlParserSqlValidator` enforces SELECT-only,
  table whitelist, multi-statement rejection and `LIMIT` injection/capping; `JdbcQueryExecutor`
  runs the hardened query read-only with a timeout; exposed to the agent as `QueryDatabaseTool`.
- Demo e-commerce dataset + 15 unit tests (agent loop control flow + SQL safety).

> Not yet: true token-level streaming from the model (the SSE layer still splits the final
> answer); a read-only DB role; per-tenant `workspace_id` injection into generated SQL.

### Phase 3 — RAG pipeline (complete)
- **Async indexing on Kafka** — uploading a document persists its text and publishes
  `DocumentUploadedEvent`; `KafkaDocumentIndexingConsumer` consumes it off the request path,
  and `DocumentIndexingService` chunks → embeds → stores vectors → marks the document indexed.
- **`knowledge` domain** — `ChunkingService` (sliding-window) plus `EmbeddingService` and
  `VectorSearchRepository` ports.
- **Embedding adapters** — `HashingEmbeddingService` (local, deterministic, no cost, default)
  and `OpenAiEmbeddingService` (`text-embedding-3-small`); switch via `EMBEDDING_PROVIDER`.
- **pgvector retrieval** — `VectorSearchRepositoryAdapter` does workspace-scoped cosine search
  (IVFFlat index); exposed to the agent as `SearchDocumentsTool`. The agent picks
  `search_documents` for unstructured questions and `query_database` for structured ones.

> Note: the local hashing embedder approximates relevance by word overlap; set
> `EMBEDDING_PROVIDER=openai` for real semantic search. PDF parsing (text/markdown only for now)
> and chunk re-ranking are future refinements.

### Phase 4 — Deploy
Dockerfile, Docker Compose (Postgres + pgvector + Kafka + app), Kubernetes manifests
(Deployment, Service, ConfigMap, HorizontalPodAutoscaler), and GitHub Actions CI pipeline.
