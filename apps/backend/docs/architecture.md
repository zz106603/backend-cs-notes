# 현재 아키텍처

## 문서 목적

이 문서는 현재 코드가 제공하는 기능과 책임 경계를 설명한다. 단계별 구현 기록과 검토 과정은 [RAG 기반 설계 결정](rag-foundation.md), Cohere 관련 세부 정책은 [Cohere Reranker 도입 정책](cohere-reranker-policy.md)에 남긴다.

프로젝트의 중심은 Java/Spring 백엔드다. 외부 AI API는 교체 가능한 어댑터로 연결하고, 문서 접근 권한, 비용 제한, 캐시, 실패 처리는 애플리케이션 서비스가 담당한다.

## 전체 구성

```text
사용자
  │
  ▼
React + Vite
  │ REST API
  ▼
Spring Boot
  ├─ Document ───── 로컬 Markdown 작성·조회·휴지통
  ├─ Auth ───────── Google OIDC, 세션, CSRF, 사용자 식별
  └─ RAG
      ├─ Indexing ─ 청킹, 변경 탐지, 임베딩, 증분 동기화
      ├─ Search ─── Dense + Sparse + RRF, 권한 필터
      ├─ Reranking  Cohere 재정렬과 RRF fallback
      ├─ Answer ─── Context 조립, 답변 생성, 출처·비용 기록
      └─ Evaluation 검색 모드별 품질 비교
          │
          ├──────── PostgreSQL + pgvector
          ├──────── OpenAI Embedding/Chat API
          └──────── Cohere Rerank API(선택)
```

현재 `compose.yaml`은 PostgreSQL과 pgvector만 실행한다. Spring Boot와 프론트엔드는 개발 호스트에서 실행하며, Python LLM 서버와 Kubernetes 구성은 아직 구현되지 않았다.

## 패키지와 책임

| 패키지 | 책임 | 주요 경계 |
| --- | --- | --- |
| `document` | Markdown 파일의 작성, 조회, 경로 검증 | `DocumentService` |
| `document.metadata` | 소유자, 공개 범위, 삭제 상태와 파일 메타데이터 동기화 | `DocumentAccessService`, `DocumentMetadataRepository` |
| `auth` | Google OIDC 사용자와 내부 사용자 UUID 연결 | Spring Security, `AppUserRepository` |
| `rag.chunk` | 제목 구조를 보존한 Markdown 청킹 | `MarkdownChunker` |
| `rag.embedding` | 문서·질의 임베딩 제공자 분리 | `EmbeddingProvider` |
| `rag.persistence` | Chunk 저장, pgvector 검색, PostgreSQL FTS | `ChunkVectorStore` |
| `rag.indexing` | dry-run, 변경분 계산, 벡터 재사용과 저장 동기화 | `RagIndexingService` |
| `rag.search` | Dense/Sparse/Hybrid 검색과 RRF 결합 | `RagSearchService` |
| `rag.reranking` | Hybrid 후보의 선택적 재정렬 | `ChunkReranker` |
| `rag.answer` | Context 제한, 답변 생성, 출처·비용·사용량 관리 | `RagAnswerGenerator`, `RagAnswerService` |
| `rag.evaluation` | 저장된 평가 질문으로 검색 모드 비교 | `RagEvaluationService` |

Spring의 조건부 설정은 기능 플래그와 API Key 유무에 따라 어댑터와 API를 활성화한다. 도메인 및 애플리케이션 서비스는 OpenAI나 Cohere의 응답 타입을 직접 사용하지 않는다.

## 색인 흐름

```text
POST /api/rag/index?dryRun=true (기본)
  → Markdown 문서 조회
  → 제목 기반 Chunk 생성
  → 현재 DB 상태와 내용 해시 비교
  → 신규 임베딩/재사용/삭제 대상과 예상 입력량 반환

POST /api/rag/index?dryRun=false
  → 동일한 계획과 한도 검증
  → 누락된 Chunk만 OpenAI Embedding 호출
  → 같은 모델·본문 해시의 기존 벡터 재사용
  → 문서 단위 Chunk 교체 및 삭제 문서 정리
```

색인은 명시적으로 실행하며 문서 저장 이벤트에 자동 연결하지 않는다. 프로세스 내부 `AtomicBoolean`으로 같은 인스턴스의 중복 실행을 막는다. 여러 인스턴스를 동시에 운영할 경우에는 DB 기반 분산 잠금이나 단일 작업 실행 정책이 추가로 필요하다.

## 검색과 답변 흐름

```text
질문
  ├─ Dense: OpenAI 질의 임베딩 → pgvector cosine 검색
  └─ Sparse: PostgreSQL FTS
          ↓
     Chunk ID로 통합
          ↓
       RRF 결합
          ↓
  Cohere Reranker(선택)
   ├─ 성공: 관련성 점수로 재정렬·임계값 적용
   └─ 비활성/외부 장애: RRF 결과 사용
          ↓
   제한된 Context 조립
          ↓
   OpenAI Chat 답변 + 출처
```

검색 모드는 `DENSE`, `SPARSE`, `HYBRID`로 나뉜다. 질의 임베딩과 Reranker 결과, 최종 답변은 TTL/LRU 방식으로 짧게 캐시한다. 검색 근거가 없으면 Chat API를 호출하지 않는다.

Reranker fallback은 외부 API의 인증 실패, rate limit, timeout, 5xx와 잘못된 외부 응답에 적용된다. Chunk ID 중복이나 범위를 벗어난 점수처럼 내부 계약 위반으로 판단되는 오류는 숨기지 않는다. OpenAI 답변 생성 실패는 성공 답변으로 대체하지 않고 `502 Bad Gateway`로 반환한다.

## 권한 경계

`CS_NOTES_SECURITY_ENABLED=true`이면 `/api/**`는 인증된 세션을 요구한다. Controller가 OIDC 사용자의 내부 UUID를 서비스에 전달하고, `PgVectorChunkStore`가 SQL 단계에서 다음 조건을 적용한다.

```text
삭제되지 않은 문서
AND (요청 사용자가 소유한 문서 OR 공개 문서)
```

답변 캐시 키에도 사용자 ID를 포함해 다른 사용자의 검색 근거가 재사용되지 않게 한다. 보안 기능이 비활성인 로컬 모드에서는 기존 Markdown 저장소 전체를 대상으로 동작한다.

## 운영 보호 장치와 현재 제약

- 기능은 기본적으로 비활성화되어 있으며 필요한 기능 플래그와 Key가 있을 때만 외부 API를 사용한다.
- 색인 전 dry-run, 실행당 문서·Chunk·문자 수 제한, 벡터 재사용으로 임베딩 비용을 제한한다.
- 답변은 Context와 출력 토큰을 제한하고, 일일 예상 비용과 진행 중 요청의 예약 비용을 함께 검사한다.
- 질문 원문은 사용량 테이블에 저장하지 않고 SHA-256 해시, 모델, 토큰, 예상 비용과 실패 유형을 기록한다.
- 캐시, 답변 비용 예약, 중복 답변·색인 차단은 현재 프로세스 메모리에 있다. 수평 확장 시 공유 캐시와 분산 동시성 제어가 필요하다.
- Cohere 장애는 검색 품질 저하로 처리할 수 있지만, OpenAI Embedding 장애는 Dense/Hybrid 검색과 색인을 사용할 수 없게 한다.

## 다음 아키텍처 단계

1. Python 기반 Open-weight LLM inference server를 독립 서비스로 추가한다.
2. 현재 `RagAnswerGenerator` 역할을 일반화해 외부 API와 자체 서빙 모델을 선택할 수 있는 Spring LLM Provider 경계를 정의한다.
3. 기존 RAG 또는 MCP를 한두 개의 명시적 Tool로 노출하고 실행 권한, timeout, 결과 크기 제한을 Spring에서 관리한다.
4. Spring, 프론트엔드, PostgreSQL, LLM Server를 Docker Compose로 통합한 뒤 Kubernetes의 health check, 설정·Secret, 자원 제한과 배포 단위를 정리한다.

각 단계는 기존 인터페이스를 우선 활용하고, 실제 교체 요구가 생기기 전에는 대규모 패키지 재구성을 하지 않는다.
