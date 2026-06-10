# Workmate

직원이 자연어로 질문하면, **사내 데이터베이스와 문서를 알아서 찾아 답해주는** 멀티테넌트
백엔드입니다. 답변은 SSE로 한 토큰씩 실시간 스트리밍합니다.

질문 하나에 정형 데이터(DB)와 비정형 지식(문서)이 동시에 필요할 때, 에이전트가 "지금
DB를 봐야 하나, 문서를 봐야 하나"를 스스로 판단해 도구를 골라 답을 만듭니다.

---

## 주요 기능

- **에이전트 채팅 (Tool Use)**
  LLM이 질문을 받아 어떤 도구를 쓸지 스스로 정하고, 서버가 그 도구를 실행해 결과를 다시
  LLM에 넣는 루프(Agent Loop)로 답을 완성합니다. 무한 호출을 막는 최대 반복 제한이 있습니다.

- **데이터 조회 (Text-to-SQL)**
  자연어 질문을 SQL로 바꿔 업무 DB를 조회합니다. `SELECT` 전용 · 테이블 화이트리스트 ·
  `LIMIT` 강제 · 다중 문장 차단 등으로 **검증·교정한 쿼리만** 읽기 전용으로 실행합니다.

- **지식 검색 (RAG)**
  업로드한 문서를 잘게 쪼개(청킹) 임베딩으로 바꿔 pgvector에 저장하고, 질문과 의미가
  가까운 문서 조각을 찾아 답변 근거로 씁니다. 무거운 인덱싱은 Kafka로 비동기 처리합니다.

- **SSE 스트리밍** — 답변을 토큰 단위로 쪼개 SSE로 전송합니다. (지금은 완성된 답변을
  분할해 보내며, 모델의 실시간 토큰 스트리밍은 향후 과제입니다.)

- **멀티테넌시** — 모든 데이터를 워크스페이스(테넌트) 단위로 격리합니다.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 / 빌드 | Java 17, Gradle |
| 프레임워크 | Spring Boot 3.2, Spring WebFlux (SSE) |
| 아키텍처 | Hexagonal (Ports & Adapters) + DDD + CQRS |
| 데이터베이스 | PostgreSQL + pgvector |
| 영속성 | Spring Data JPA |
| 메시징 | Apache Kafka |
| LLM | Claude (로컬 CLI / Mock 전환 가능) |
| 임베딩 | Hashing(기본·로컬 근사) · Ollama(로컬 의미검색) — OpenAI 어댑터는 작성만, 미검증 |
| 배포 | Docker, Kubernetes, GitHub Actions(CI), GHCR |
| 모니터링 | Spring Actuator (헬스 체크 + Prometheus 포맷 메트릭 노출) |

> LLM·임베딩·스토리지는 모두 **포트 뒤 어댑터**로 분리돼 있어, 도메인 코드 변경 없이
> 구현체를 갈아끼울 수 있습니다(예: 임베딩을 Hashing ↔ Ollama로 전환). OpenAI 어댑터도
> 같은 포트에 끼우도록 작성돼 있지만, 키가 없어 아직 검증하지는 못했습니다.

---

## 빠른 실행

```bash
# 1) 인프라 (PostgreSQL + pgvector, Kafka)
docker compose up -d
bash db/apply-business.sh    # Text-to-SQL 데모용 업무 데이터
bash db/apply-vector.sh      # pgvector 문서 청크 테이블

# 2) 애플리케이션
SPRING_JPA_HIBERNATE_DDL_AUTO=update ./gradlew bootRun

# 3) 질문해보기 (SSE 스트리밍)
curl -N -X POST localhost:8080/api/v1/chat/messages \
  -H 'Content-Type: application/json' -H "X-Workspace-Id: $(uuidgen)" \
  -d '{"content":"환불이 가장 많았던 제품 top 3를 알려줘"}'
```

- 기본값은 **로컬 Claude CLI + 해싱 임베딩**으로 비용 없이 동작합니다.
- 진짜 의미 기반 검색은 `EMBEDDING_PROVIDER=ollama`, 완전 오프라인 데모는 `LLM_PROVIDER=mock`.

---

## 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/v1/chat/messages` | 채팅 — 답변을 SSE로 스트리밍 |
| `GET`  | `/api/v1/conversations/{id}` | 대화 기록 조회 |
| `POST` | `/api/v1/documents` | 문서 업로드 (비동기 인덱싱, `202`) |
| `GET`  | `/api/v1/documents/{id}` | 문서·인덱싱 상태 조회 |
| `GET`  | `/actuator/health` | 헬스 체크 |
| `GET`  | `/swagger-ui.html` | API 문서 (Swagger UI) |

모든 요청은 테넌트 식별을 위해 `X-Workspace-Id: <UUID>` 헤더가 필요합니다.

---

## 배포

멀티스테이지 `Dockerfile`로 단일 이미지를 만들고, `k8s/` 매니페스트로 **API 서버와 인덱싱
워커를 분리 배포**해 워커만 독립적으로 스케일할 수 있습니다. GitHub Actions가 push마다
빌드·테스트하고, `main`에서는 이미지를 GHCR로 푸시합니다.
