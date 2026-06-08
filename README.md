# Workmate

A multi-tenant agentic AI platform that lets employees ask questions in natural language and
receive answers drawn from internal documents (**RAG**) and internal databases (**Text-to-SQL**),
streamed back token-by-token over **Server-Sent Events (SSE)**.

For full background and motivation see the planning documents at the repo root:
- `Workmate-프로젝트-기획.md` — product concept, use-case scenarios, bounded contexts
- `Workmate-OMC-실행계획.md` — phased execution plan and OMC orchestration strategy

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
│  infrastructure/kafka     Event publisher adapter        │
│  infrastructure/llm       Claude / OpenAI clients        │
│  infrastructure/storage   S3 document storage            │
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
| Build           | Gradle 9                                        |
| Framework       | Spring Boot 3.2, Spring WebFlux (reactive SSE)  |
| Persistence     | Spring Data JPA, PostgreSQL 16 + pgvector       |
| Messaging       | Apache Kafka (Spring Kafka)                     |
| Observability   | Spring Actuator, Micrometer, Prometheus         |
| API docs        | SpringDoc OpenAPI 3 (Swagger UI)                |
| LLM             | Anthropic Claude (Phase 2), OpenAI Embeddings (Phase 3) |

---

## Getting Started

### Prerequisites

- JDK 17+
- Docker (for PostgreSQL + Kafka)

### 1. Start infrastructure

```bash
docker run -d --name workmate-postgres \
  -e POSTGRES_DB=workmate \
  -e POSTGRES_USER=workmate \
  -e POSTGRES_PASSWORD=workmate \
  -p 5432:5432 ankane/pgvector

docker run -d --name workmate-kafka \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -p 9092:9092 bitnami/kafka:latest
```

> PostgreSQL must have the **pgvector** extension enabled. The `ankane/pgvector` image
> includes it. Run `CREATE EXTENSION IF NOT EXISTS vector;` if using a plain Postgres image.

### 2. Build

```bash
./gradlew build
```

### 3. Run

```bash
./gradlew bootRun
```

The server starts on **http://localhost:8080**.

### Environment variables (optional overrides)

| Variable                  | Default                              | Description                  |
|---------------------------|--------------------------------------|------------------------------|
| `DATASOURCE_URL`          | `jdbc:postgresql://localhost:5432/workmate` | JDBC URL             |
| `DATASOURCE_USERNAME`     | `workmate`                           | DB username                  |
| `DATASOURCE_PASSWORD`     | `workmate`                           | DB password                  |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                     | Kafka broker address         |
| `ANTHROPIC_API_KEY`       | _(empty)_                            | Claude API key (Phase 2)     |
| `OPENAI_API_KEY`          | _(empty)_                            | OpenAI API key (Phase 3)     |
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
│   │   ├── agent/                        # Tool, ToolResult interfaces
│   │   └── database/                     # SqlQuery value object
│   ├── application/
│   │   ├── chat/                         # ChatService, SendMessageCommand, ConversationView …
│   │   └── document/                     # DocumentUploadService, UploadDocumentCommand …
│   ├── infrastructure/
│   │   ├── jpa/                          # JPA entities, repository adapters (Phase 1+)
│   │   ├── kafka/                        # Event publisher adapter
│   │   ├── llm/                          # Claude / OpenAI clients (Phase 2-3)
│   │   └── storage/                      # S3 adapter (Phase 2)
│   └── interfaces/rest/
│       ├── ChatController.java
│       ├── DocumentController.java
│       ├── RestExceptionHandler.java
│       └── dto/                          # Request / Response records
├── src/main/resources/
│   └── application.yml
├── build.gradle
├── Workmate-프로젝트-기획.md
└── Workmate-OMC-실행계획.md
```

---

## Roadmap

### Phase 1 — DDD skeleton (complete)
Hexagonal package structure, domain aggregates (Workspace, Conversation, Document),
application services with CQRS (ChatService, DocumentUploadService), REST controllers
with SSE stub, configuration, and this documentation.

### Phase 2 — Text-to-SQL + Agent Loop
Implement the Agent Loop (`while tool_calls`) against Claude API (Anthropic SDK).
Add the `database` context: natural-language → SQL conversion via Claude, JSqlParser
safety validation (read-only account, table whitelist, `LIMIT` injection, DDL blocking),
and `QueryDatabaseTool`. Stream real tokens from Claude instead of the current
space-split stub.

### Phase 3 — RAG pipeline
Document ingestion pipeline on Kafka: chunk PDFs/Markdown with
`RecursiveCharacterTextSplitter`, generate embeddings via OpenAI API, store in pgvector.
At query time retrieve top-k nearest chunks and inject as context into the Agent Loop.
Add `KnowledgeSearchTool`.

### Phase 4 — Deploy
Dockerfile, Docker Compose (Postgres + pgvector + Kafka + app), Kubernetes manifests
(Deployment, Service, ConfigMap, HorizontalPodAutoscaler), and GitHub Actions CI pipeline.
