# Cohere Reranker 도입 정책

## 문서 목적

이 문서는 Hybrid 검색의 두 번째 단계에 Cohere Rerank API를 연결하기 전에 합의한 운영 정책을 기록한다. 특정 SDK의 사용법보다 다음 질문에 답하는 것을 우선한다.

- Reranker가 현재 Dense, Sparse, RRF 파이프라인에서 어떤 역할을 맡는가?
- 무료 Trial Key의 제한 안에서 호출량을 어떻게 관리하는가?
- API Key 누락이나 Cohere 장애가 왜 전체 검색 장애로 이어지면 안 되는가?
- Python Cross-Encoder 대신 외부 API를 선택한 이유는 무엇인가?
- 관련성 점수의 임계값은 언제, 어떤 근거로 결정하는가?

## 현재 검색 파이프라인과 목표

현재 Hybrid 검색은 다음 순서로 동작한다.

```text
질문
  ├─ Dense: OpenAI 질의 임베딩 + pgvector cosine 검색 최대 20개
  └─ Sparse: PostgreSQL FTS 검색 최대 20개
                 ↓
          Chunk ID 기준 중복 통합
                 ↓
             RRF 상위 20개
                 ↓
         요청 limit만큼 최종 반환
```

RRF는 서로 다른 점수 체계를 직접 더하지 않고 순위를 합치는 안전한 1차 결합 방법이다. 하지만 질문과 Chunk 본문을 직접 비교하지 않으므로 양쪽 검색에 애매하게 등장한 결과가 한쪽 검색의 정확한 결과보다 높아질 수 있다.

Cohere Reranker를 연결한 목표 흐름은 다음과 같다.

```text
Dense + Sparse
      ↓
RRF 상위 20개 후보
      ↓
Cohere가 질문과 각 Chunk를 직접 비교
      ↓
관련성 점수 검증·재정렬·선택적 임계값 필터
      ↓
요청 limit만큼 최종 반환
```

Reranker는 Dense나 Sparse를 대체하지 않는다. 첫 단계 검색이 넓고 빠르게 후보를 찾고, Reranker는 제한된 후보의 정밀한 순서를 결정한다.

## 모델과 API 선택

초기 모델은 `rerank-v4.0-fast`를 사용한다.

- 한국어를 포함한 다국어 검색을 지원한다.
- `pro`보다 낮은 지연시간과 높은 처리량을 목표로 한다.
- 현재 후보가 최대 20개인 개인 학습 서비스에 충분한 품질과 응답시간을 기대할 수 있다.
- 모델명은 환경변수로 분리하여 이후 `rerank-v4.0-pro` 또는 다른 구현체와 비교할 수 있게 한다.

API는 Cohere Rerank v2의 `POST /v2/rerank`를 사용한다. 모델에는 RRF 상위 20개 전체를 전달하고 초기에는 모든 후보의 관련성 점수를 받는다. 일부 결과만 먼저 잘라 받으면 낮은 점수 분포를 관찰하기 어렵고 적절한 임계값을 정할 근거도 부족하기 때문이다.

참고:

- <https://docs.cohere.com/v2/reference/rerank>
- <https://docs.cohere.com/changelog/rerank-v4.0>
- <https://docs.cohere.com/v2/docs/rerank-overview>

## Trial Key 사용 정책

Cohere Trial Key는 무료 평가와 프로토타입 용도에 적합하지만 호출 제한이 있다. 문서 작성 시점의 공식 제한은 Rerank 분당 10회, Trial 전체 월 1,000회다. 제한은 변경될 수 있으므로 구현 또는 운영 설정을 바꾸기 전에 공식 문서를 다시 확인한다.

참고: <https://docs.cohere.com/v2/docs/rate-limits>

호출량 보호 원칙은 다음과 같다.

- 검색어 입력 중에는 호출하지 않고 사용자가 검색을 제출했을 때만 호출한다.
- 동일한 질문과 동일한 후보 Chunk 조합은 TTL 캐시를 재사용한다.
- 평가 실행 한 번에서는 Hybrid 결과에 대해서만 한 번 호출한다.
- `429 Too Many Requests`에 즉시 재시도하지 않는다.
- 초기 구현에는 자동 재시도를 넣지 않고 기존 RRF 결과로 즉시 복귀한다.
- 후보는 RRF 상위 20개로 제한한다.
- 호출 횟수, 후보 수, 처리 시간, fallback 사유를 기록하되 질문과 Chunk 본문은 로그에 남기지 않는다.

권장 초기값은 다음과 같다.

```env
RAG_RERANKING_ENABLED=false
RAG_RERANKING_MINIMUM_SCORE=0.65
COHERE_API_KEY=
COHERE_RERANK_MODEL=rerank-v4.0-fast
RAG_SEARCH_HYBRID_CANDIDATE_LIMIT=20
COHERE_RERANK_MAX_TOKENS_PER_DOCUMENT=1024
COHERE_RERANK_CACHE_TTL=10m
COHERE_RERANK_CACHE_MAX_ENTRIES=100
COHERE_RERANK_CONNECT_TIMEOUT=2s
COHERE_RERANK_READ_TIMEOUT=5s
COHERE_RERANK_MAX_REQUESTS_PER_MINUTE=8
```

`COHERE_API_KEY`는 프로세스 환경변수로 주입하며 저장소, `application.yml`, 로그, 오류 응답에 실제 값을 기록하지 않는다.

## 활성화 정책

Reranker는 검색의 필수 구성 요소가 아니라 선택적인 품질 개선 기능이다. 기본값은 반드시 `false`로 둔다.

| 활성화 값 | API Key | 동작 |
| --- | --- | --- |
| `false` | 없음 | Cohere를 호출하지 않고 RRF 결과 사용 |
| `false` | 있음 | Cohere를 호출하지 않고 RRF 결과 사용 |
| `true` | 있음 | Cohere 호출 후 재정렬 |
| `true` | 없음 | 경고 로그를 남기고 RRF 결과 사용 |

`true`인데 Key가 없는 상태를 애플리케이션 시작 실패로 만들지는 않는다. 전체 문서 검색은 계속 사용할 수 있어야 하지만, 운영자가 Cohere가 동작한다고 오해하지 않도록 명확한 경고를 남긴다.

## 장애와 fallback 정책

Cohere 장애는 전체 검색 장애로 전파하지 않는다. 다음 경우에는 Reranker 적용 전 RRF 순서를 그대로 반환한다.

- API Key 누락, 만료 또는 인증 실패
- 분당 또는 월간 한도 초과
- 연결 실패와 응답 시간 초과
- Cohere `5xx` 또는 일시적인 서비스 장애
- 응답 본문 누락이나 JSON 파싱 실패

```text
Cohere 정상                 → 관련성 점수로 재정렬
Cohere 비활성·누락·장애     → 기존 RRF 순서로 반환
```

fallback은 외부 서비스 장애에만 적용한다. 다음과 같은 내부 계약 위반은 조용히 숨기지 않는다.

- 요청하지 않은 Chunk ID를 구현체가 반환함
- 같은 Chunk ID를 중복 반환함
- `NaN`, 무한대 또는 0~1 범위 밖의 점수를 반환함

이런 오류는 어댑터 구현 버그일 가능성이 높으므로 테스트나 오류 로그를 통해 발견해야 한다. 외부 통신 실패와 내부 프로그래밍 오류를 같은 예외로 취급하지 않는다.

## Cohere에 전달할 문서 형식

본문만 전달하면 같은 문서의 여러 Chunk를 구분하거나 제목과 섹션 정보를 활용하기 어렵다. 각 후보는 다음과 유사한 YAML 문자열로 구성한다.

```yaml
title: 스프링 트랜잭션 전파 속성
path: 백엔드/Spring/스프링 트랜잭션 전파 속성.md
section: 트랜잭션 전파 > REQUIRES_NEW
content: 기존 트랜잭션을 일시 중단하고 새로운 트랜잭션을 시작한다...
```

Cohere는 구조화된 문서를 YAML 문자열로 표현하는 방식을 권장한다. Chunk ID는 API 응답의 `index`를 원래 후보와 다시 연결하는 내부 식별에 사용하며 모델 판단용 본문에는 반드시 포함할 필요가 없다.

## 관련성 임계값 정책

초기 평가에서는 `RAG_RERANKING_MINIMUM_SCORE`를 `0.0`으로 두고 점수 분포를 수집했다. 긍정 질문의 정답 Chunk 최솟값 `0.770`과 부정 질문의 최고 점수 `0.558` 사이를 기준으로 운영 기본값을 `0.65`로 정했다.

이 값은 현재 문서와 평가 질문에 대한 1차 기준이다. 문서, 후보 구성, Cohere 모델이 바뀌면 긍정 정답 점수와 부정 최고 점수를 다시 측정해 조정한다.

먼저 검색 품질 평가 화면에서 다음 값을 수집한다.

- 정답 문서 Chunk의 Cohere 점수와 재정렬 순위
- 관련은 있지만 보조적인 Chunk의 점수
- 문서에 답이 없는 부정 질문에서 가장 높은 오탐 점수
- RRF 순위와 Cohere 순위가 달라진 사례

긍정 질문의 정답 점수와 부정 질문의 오탐 점수가 어느 정도 분리되는지 확인한 뒤 임계값을 정한다. 임계값 변경 전후에는 같은 평가 질문 집합으로 Recall@K, 첫 정답 순위, MRR, 부정 질문의 통과 여부를 다시 비교한다.

RRF 점수는 후보 순서를 합친 값이지 절대 관련성이 아니므로 Reranker 임계값 대신 사용하지 않는다.

## Python Cross-Encoder를 선택하지 않은 이유

Python Cross-Encoder는 질문과 문서를 함께 입력해 관련성을 계산하므로 Reranker 학습에 매우 적합하다. 외부 호출 비용이 없고 모델과 추론 설정을 직접 통제할 수 있다는 장점도 있다. 따라서 기술적으로 열등해서 제외한 것이 아니다.

현재 프로젝트에서 우선 선택하지 않은 이유는 다음과 같다.

### 별도 추론 런타임 부담

현재 서비스의 실행 구성은 Spring Boot, React, PostgreSQL이다. Cross-Encoder를 사용하면 Python 환경, 모델 파일, PyTorch 또는 유사한 추론 런타임과 별도 API 서버가 추가된다. 로컬 개발과 향후 배포에서 프로세스 관리, 헬스 체크, 버전 호환성, 로그 수집 대상이 함께 늘어난다.

### 로컬 자원 사용량

Cross-Encoder는 후보마다 질문과 Chunk를 함께 추론한다. 최대 20개 후보를 CPU에서 처리하면 응답시간이 길어질 수 있고, GPU를 사용하면 별도 하드웨어와 드라이버 관리가 필요하다. 사용자는 이미 로컬 모델 실행의 자원 부담보다 외부 API 사용을 선호한다.

### 현재 학습 목표

이번 단계의 목표는 모델 서빙 자체보다 다음 실무 경계를 경험하는 것이다.

- 1차 검색과 2차 재정렬 분리
- 외부 API 어댑터와 도메인 인터페이스 분리
- timeout, rate limit, fallback, 캐시 설계
- 관련성 점수 평가와 임계값 보정
- 장애가 핵심 검색 기능으로 전파되지 않는 graceful degradation

Cohere API는 모델 서버 운영을 먼저 해결하지 않고도 이 검색 파이프라인과 운영 정책을 집중적으로 학습할 수 있다.

### 선택을 고정하지 않음

도메인 코드는 `ChunkReranker` 인터페이스에만 의존한다. 따라서 향후 동일한 평가 데이터로 다음 구현을 교체 비교할 수 있다.

```text
CohereChunkReranker
PythonCrossEncoderChunkReranker
다른 외부 Rerank API 구현체
```

추후 다음 조건이 생기면 Python Cross-Encoder를 다시 검토한다.

- 외부 API 호출 제한이나 개인정보 전송 정책이 문제가 됨
- 충분한 CPU/GPU 환경을 확보함
- 자체 모델 선택, 양자화, 배치 추론을 학습하려는 목표가 생김
- Cohere와 로컬 모델을 동일 평가 데이터로 비용·지연시간·품질 비교하려 함

## 구현 및 검증 순서

1. Cohere HTTP 어댑터와 조건부 설정을 추가한다.
2. Key 누락과 API 장애에서 RRF fallback이 동작하는지 테스트한다.
3. 질문·후보 조합 캐시와 timeout을 적용한다.
4. 선택적으로 실행하는 실제 Trial Key 연결 테스트를 추가한다.
5. 평가 화면에 RRF 순위, Cohere 순위, Cohere 점수를 표시한다.
6. 기존 긍정·부정 평가 질문으로 점수 분포를 수집한다.
7. 평가 결과에 근거해 최소 관련성 점수를 조정한다.
8. 안정화된 후 RAG 답변 Context에도 재정렬 결과를 사용한다.

실제 API를 한 번 호출했다는 사실만으로 도입 완료로 판단하지 않는다. 기존 RRF보다 검색 품질이 개선되는지, 부정 질문에서 낮은 관련성 후보를 구분할 수 있는지, 장애 시 검색이 유지되는지를 함께 확인해야 한다.

## 현재 구현 상태

다음 항목까지 코드에 반영했다.

- Cohere Rerank v2 HTTP 어댑터와 `rerank-v4.0-fast` 기본 설정
- `RAG_RERANKING_ENABLED=true`이면서 API Key가 있을 때만 생성되는 조건부 Bean
- Key 누락, HTTP 오류, timeout, 로컬 분당 제한, 잘못된 외부 응답의 RRF fallback
- 질문과 Chunk ID 조합을 키로 하는 TTL/LRU 캐시
- Trial 공식 제한보다 낮은 기본 8회/분 프로세스 로컬 제한
- 제목, 경로, 섹션, 본문을 포함하는 YAML 호환 문자열 요청
- 외부 호출 없는 HTTP·캐시·조건부 설정·fallback 단위 테스트
- `COHERE_API_KEY`가 있을 때만 명시적으로 실행되는 `cohereRerankerLiveTest`
- 평가 및 검색 화면의 Reranker 점수·순위 우선 표시
- 긍정·부정 평가 점수 분포에서 정한 기본 관련성 임계값 `0.65`
- 임계값을 통과한 Chunk가 없을 때 빈 검색 결과 및 RAG 답변 생성 차단
- 평가 화면의 임계값 적용 여부와 부정 질문 차단 결과 표시

실제 Trial API 평가에서 긍정 정답 최솟값 `0.770`, 부정 질문 최댓값 `0.558`을 확인했다. 현재 기본 임계값 `0.65`는 이 평가 집합에 대한 1차 기준이며, 문서와 질문이 늘어나면 같은 방식으로 재평가한다.
