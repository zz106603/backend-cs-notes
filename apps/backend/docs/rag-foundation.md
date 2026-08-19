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

## 다음 단계

M4.3에서 Spring AI 기반 임베딩 어댑터와 PostgreSQL·pgvector 저장 어댑터를 구현한다. 그 전에 사용할 임베딩 모델을 결정하고 차원, 한국어 검색 품질, 비용, 문서·질의 input type 지원 여부를 기록한다.
