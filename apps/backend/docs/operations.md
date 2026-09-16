# 실행 및 운영 가이드

## 실행 모드

기본 문서 기능은 PostgreSQL과 외부 AI API 없이 실행할 수 있다.

```powershell
./gradlew.bat :apps:backend:bootRun

cd apps/frontend
npm install
npm run dev
```

RAG를 사용하려면 PostgreSQL을 먼저 실행한다.

```powershell
docker compose up -d postgres
```

그다음 같은 터미널 세션에서 필요한 환경 변수를 설정하고 백엔드를 실행한다. 저장소나 `application.yml`에 실제 API Key를 기록하지 않는다.

## 기능별 활성화 조건

| 기능 | 필수 설정 | 외부 호출 | 비활성 또는 누락 시 |
| --- | --- | --- | --- |
| PostgreSQL 저장소 | `RAG_PERSISTENCE_ENABLED=true` | 없음 | Markdown 기본 기능만 사용 |
| 색인 | 저장소 + `RAG_INDEXING_ENABLED=true` + `OPENAI_API_KEY` | OpenAI Embedding | 색인 API 미생성 |
| Sparse 검색 | 저장소 + `RAG_SEARCH_ENABLED=true` | 없음 | 검색 API 미생성 |
| Dense/Hybrid 검색 | 검색 기능 + `OPENAI_API_KEY` | OpenAI Embedding | Sparse만 사용 가능, Dense 요청은 검증 오류 |
| Reranker | 검색 기능 + `RAG_RERANKING_ENABLED=true` + `COHERE_API_KEY` | Cohere Rerank | RRF 순서로 fallback |
| RAG 답변 | 저장소 + 검색 + `RAG_ANSWER_ENABLED=true` + `OPENAI_API_KEY` | OpenAI Embedding/Chat, Cohere(선택) | 답변 API 미생성 |
| 인증·권한 | 저장소 + `CS_NOTES_SECURITY_ENABLED=true` + Google OAuth 설정 | Google OIDC | 로컬 비인증 모드 |

`RAG_EVALUATION_ENABLED`의 기본값은 `true`지만 저장소와 검색이 활성화된 경우에만 평가 API가 생성된다.

## 로컬 RAG 실행 예시

```powershell
$env:OPENAI_API_KEY = "발급받은 API 키"
$env:RAG_PERSISTENCE_ENABLED = "true"
$env:RAG_INDEXING_ENABLED = "true"
$env:RAG_SEARCH_ENABLED = "true"
$env:RAG_ANSWER_ENABLED = "true"

./gradlew.bat :apps:backend:bootRun
```

Hybrid 검색을 Cohere로 재정렬하려면 다음 설정을 추가한다.

```powershell
$env:COHERE_API_KEY = "발급받은 API 키"
$env:RAG_RERANKING_ENABLED = "true"
```

## 주요 운영 설정

| 목적 | 환경 변수 | 기본값 |
| --- | --- | --- |
| 임베딩 모델·차원 | `RAG_EMBEDDING_MODEL`, `RAG_EMBEDDING_DIMENSIONS` | `text-embedding-3-small`, `1536` |
| 색인 입력 상한 | `RAG_INDEXING_MAX_DOCUMENTS`, `RAG_INDEXING_MAX_CHUNKS_PER_DOCUMENT`, `RAG_INDEXING_MAX_CHARACTERS_PER_RUN` | `200`, `200`, `500000` |
| Hybrid 후보와 RRF | `RAG_SEARCH_HYBRID_CANDIDATE_LIMIT`, `RAG_SEARCH_HYBRID_RRF_K` | `20`, `60` |
| Reranker 임계값 | `RAG_RERANKING_MINIMUM_SCORE` | `0.65` |
| Reranker timeout | `COHERE_RERANK_CONNECT_TIMEOUT`, `COHERE_RERANK_READ_TIMEOUT` | `2s`, `5s` |
| 답변 Context·출력 상한 | `RAG_ANSWER_MAX_CONTEXT_CHARACTERS`, `RAG_ANSWER_MAX_OUTPUT_TOKENS` | `12000`, `600` |
| 답변 일일 예상 비용 | `RAG_ANSWER_DAILY_COST_LIMIT_USD` | `0.25` |
| 비용 집계 시간대 | `RAG_ANSWER_BUDGET_ZONE_ID` | `Asia/Seoul` |

전체 설정과 기본값은 `apps/backend/src/main/resources/application.yml`을 기준으로 한다. 모델 가격이 변경되면 `RAG_ANSWER_INPUT_PRICE_PER_MILLION_USD`와 `RAG_ANSWER_OUTPUT_PRICE_PER_MILLION_USD`도 갱신해야 한다.

임베딩 모델 또는 차원을 변경하면 기존 벡터와 HNSW 인덱스를 혼용하지 않는다. 스키마 마이그레이션과 재색인 계획을 함께 준비해야 한다.

## 장애 시 동작

| 상황 | 현재 동작 | 확인할 항목 |
| --- | --- | --- |
| Cohere Key 누락·429·timeout·5xx | RRF 결과 반환 | fallback 사유와 Cohere 설정 |
| 검색 근거 없음 | Chat API 호출 없이 응답 | 검색 모드, 임계값, 색인 상태 |
| 일일 예상 비용 초과 | 답변 요청을 `429`로 차단 | 사용량 테이블과 단가 설정 |
| 동일 답변 생성 중 | 중복 요청을 `409`로 차단 | 장시간 실행 또는 외부 API 지연 |
| OpenAI 답변 실패 | `502` 반환, 실패 사용량 기록 | API 상태, Key, 모델과 실패 유형 |
| PostgreSQL 연결 실패 | RAG 관련 Bean 기동 실패 | DB health check, URL과 인증정보 |

로그에는 요청 ID, 모델, 토큰, 예상 비용, 처리 시간과 실패 유형을 남긴다. 질문 원문, Chunk 본문, API Key는 운영 로그에 남기지 않는다.

## 검증

외부 API를 호출하지 않는 기본 테스트는 다음 명령으로 실행한다.

```powershell
./gradlew.bat :apps:backend:test
```

라이브 테스트는 해당 Key와 로컬 PostgreSQL을 준비한 뒤 목적에 맞는 Gradle task를 명시적으로 실행한다.

```powershell
./gradlew.bat :apps:backend:openAiLiveTest
./gradlew.bat :apps:backend:ragSearchLiveTest
./gradlew.bat :apps:backend:ragAnswerLiveTest
./gradlew.bat :apps:backend:cohereRerankerLiveTest
```

라이브 테스트는 외부 API 비용을 발생시킬 수 있으며 기본 `test` task에서는 제외되어 있다.
