# RAG 기반 설계 결정

## 현재 결정

M4.1과 M4.2에서는 LangChain, LangChain4j 또는 Spring AI를 직접 의존하지 않는다. 먼저 Markdown 청킹과 임베딩 제공자 포트를 애플리케이션 코드로 구현한다.

이 결정은 프로젝트가 작기 때문이 아니다. 다음 학습 목표를 우선하기 때문이다.

- Markdown 구조를 어떤 기준으로 검색 단위로 바꾸는지 직접 설명할 수 있다.
- 문서용 임베딩과 질의용 임베딩이 다를 수 있음을 인터페이스에 반영한다.
- 임베딩 모델과 벡터 차원이 바뀌어도 도메인 코드가 영향을 받지 않게 한다.
- Chunk 해시와 안정적인 ID를 이용한 증분 동기화 기반을 이해한다.
- 프레임워크를 사용하기 전에 RAG의 ingestion, retrieval 경계를 직접 설계해 본다.

## 프레임워크 검토

### Python LangChain

RAG 생태계와 예제 학습에는 가치가 크지만 현재 Java/Spring Boot 서비스에 별도 Python 런타임을 추가하게 된다. 이 프로젝트의 주 구현 프레임워크로 사용하지 않는다. 추후 동일한 평가 데이터로 검색 파이프라인을 비교하는 별도 실험에는 사용할 수 있다.

### LangChain4j

Java에서 문서 로더, splitter, embedding store, retriever와 advanced RAG 구성 요소를 폭넓게 학습하기 좋다. 특히 query transformation, routing, reranking을 실습할 때 가치가 있다. 다만 M4.1~M4.2에 바로 적용하면 직접 설계하려는 경계가 프레임워크 타입에 묻힐 수 있으므로 지금은 도입하지 않는다.

참고: <https://docs.langchain4j.dev/tutorials/rag/>

### Spring AI

Spring Boot 자동 구성, `EmbeddingModel`, `VectorStore`, ETL 추상화가 현재 기술 스택과 가장 자연스럽게 연결된다. Spring 기반 백엔드 커리어에서도 설정, 관측성, 테스트, 벡터 저장소 교체를 함께 설명하기 좋다. M4.3에서 PostgreSQL·pgvector를 연결할 때 첫 번째 어댑터 후보로 사용한다.

참고:

- <https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html>
- <https://docs.spring.io/spring-ai/reference/api/vectordbs.html>

## 청킹 규칙

- Markdown `#`~`######` 제목을 섹션 경계로 사용한다.
- 각 Chunk에는 문서 ID, 경로, 제목, 태그, 제목 계층과 순서를 보존한다.
- 코드 펜스와 표는 의미가 깨지지 않도록 하나의 블록으로 유지하며 목표 크기를 초과할 수 있다.
- 긴 일반 텍스트만 줄바꿈과 문장 경계를 우선해 추가 분할한다.
- 내용의 SHA-256 해시로 변경 여부를 판단한다.
- 문서 ID, 섹션 경로, 순서, 내용 해시를 조합해 안정적인 Chunk ID를 만든다.

현재 크기 제한은 문자 기반이다. 실제 임베딩 모델을 선택한 뒤 해당 tokenizer 기준 제한을 어댑터 또는 후속 splitter에서 보완한다.

## 임베딩 경계

`EmbeddingProvider`는 다음을 보장한다.

- 문서와 질의를 다른 목적으로 임베딩할 수 있다.
- 모델 이름과 벡터 차원을 노출한다.
- 입력 순서와 결과 개수, ID, 모델, 차원을 검증한다.
- pgvector나 외부 API 타입이 도메인 계층으로 새어 나오지 않는다.

## M4.3 적용 내용

- Spring AI `EmbeddingModel`을 `EmbeddingProvider`로 변환하는 어댑터
- Chunk 단위 교체와 cosine 유사도 검색을 제공하는 `ChunkVectorStore`
- PostgreSQL 17 및 pgvector 0.8.6 로컬 Docker 환경
- Flyway 기반 `document_chunk` 테이블과 HNSW 인덱스
- DB가 없어도 기존 서비스가 실행되도록 하는 조건부 활성화

현재 스키마는 `text-embedding-3-small` 기본 차원인 1536을 기준으로 한다. 모델 또는 차원을 변경할 때는 기존 벡터와 HNSW 인덱스를 그대로 혼용하지 말고 새 마이그레이션으로 재구성해야 한다.

## M4.4 적용 내용

- Spring AI OpenAI starter와 `OpenAiEmbeddingModel`을 이용한 실제 임베딩 어댑터
- `OPENAI_API_KEY`가 있을 때만 모델을 생성하는 조건부 활성화
- 문서 내용을 로그에 노출하지 않는 배치 수, 입력 수, 처리 시간 및 실패 유형 기록
- 설정 가능한 배치 크기와 지수 백오프 재시도
- 외부 SDK 예외를 애플리케이션의 `EmbeddingProviderException`으로 변환

Spring AI는 OpenAI API를 무료로 제공하는 서비스가 아니라 SDK와 Spring Boot 통합 계층이다. 실제 임베딩 요청 비용과 사용 한도는 OpenAI 계정에 귀속된다. 애플리케이션 시작만으로는 API 요청을 보내지 않으며, 이후 색인 및 검색 유스케이스가 `EmbeddingProvider`를 호출할 때만 네트워크 요청이 발생한다.

자동 구성은 API 키가 없는 환경에서의 기동을 확실히 보장하기 위해 비활성화하고, 프로젝트의 조건부 설정에서 같은 Spring AI 모델을 직접 생성한다. 덕분에 테스트와 일반 문서 탐색은 외부 서비스 없이 실행할 수 있고, 도메인 계층은 계속 `EmbeddingProvider` 포트만 의존한다.

## 다음 단계

문서 작성·수정·휴지통 이벤트를 Chunk 생성과 임베딩 동기화에 연결한다. 이후 동일한 질의 집합으로 한국어 검색 품질, 비용, 응답 시간을 기록한다.
