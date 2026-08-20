# Backend CS Notes

백엔드 개발자를 위한 컴퓨터 공학(CS) 및 소프트웨어 공학 지식 정리 저장소입니다.

## 로컬 웹 애플리케이션

저장소의 Markdown 문서를 카테고리별로 탐색하고 읽을 수 있는 웹 애플리케이션이 `apps` 아래에 있습니다.

### 백엔드 실행

Java 21이 필요합니다. Gradle Wrapper가 포함되어 있어 Gradle을 별도로 설치할 필요는 없습니다.

```bash
./gradlew :apps:backend:bootRun
```

저장소 루트에서 실행하며, Windows PowerShell에서는 `./gradlew.bat :apps:backend:bootRun`을 사용합니다. macOS와 Linux에서는 `./gradlew :apps:backend:bootRun`을 사용합니다.

기본적으로 저장소 루트의 Markdown 문서를 읽으며 API는 `http://localhost:8080`에서 실행됩니다. 다른 문서 디렉터리를 사용하려면 `CS_NOTES_ROOT` 환경 변수를 지정할 수 있습니다.

### 프론트엔드 실행

Node.js 20.19 이상이 필요합니다.

```bash
cd apps/frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 열면 됩니다. 개발 서버는 `/api` 요청을 로컬 백엔드로 전달합니다.

### RAG용 PostgreSQL 실행

pgvector 저장소는 기본적으로 비활성화되어 있어 기존 문서 기능은 DB 없이 실행됩니다. 로컬 DB를 시작하려면 저장소 루트에서 다음을 실행합니다.

```bash
docker compose up -d postgres
```

백엔드 실행 환경에 `RAG_PERSISTENCE_ENABLED=true`를 지정하면 Flyway가 Chunk 및 1536차원 임베딩 테이블을 생성합니다. 접속 정보는 `RAG_DATABASE_URL`, `RAG_DATABASE_USERNAME`, `RAG_DATABASE_PASSWORD`로 변경할 수 있습니다.

### OpenAI 임베딩 사용

OpenAI 임베딩은 API 키가 있을 때만 활성화됩니다. 키를 설정하지 않아도 기존 문서 기능과 백엔드는 정상 실행되며, 저장소나 설정 파일에는 키를 기록하지 않습니다.

Windows PowerShell에서는 실행할 터미널 세션에 환경 변수를 설정한 뒤 백엔드를 시작합니다.

```powershell
$env:OPENAI_API_KEY = "발급받은 API 키"
./gradlew.bat :apps:backend:bootRun
```

기본 모델은 `text-embedding-3-small`, 벡터 차원은 PostgreSQL 스키마와 같은 1536입니다. 필요하면 `RAG_EMBEDDING_MODEL`, `RAG_EMBEDDING_DIMENSIONS`, `RAG_EMBEDDING_BATCH_SIZE`로 조정할 수 있지만, 차원을 변경할 때는 pgvector 스키마도 함께 마이그레이션해야 합니다. API 키를 설정하거나 서버를 시작하는 것만으로 요청이 발생하지 않으며, 문서 색인 또는 검색 흐름이 임베딩 포트를 호출할 때 비용이 발생합니다.

API 키와 실제 OpenAI 임베딩 호출을 확인하려면 전용 라이브 테스트를 실행합니다.

```powershell
$env:OPENAI_API_KEY = "발급받은 API 키"
./gradlew.bat :apps:backend:openAiLiveTest
```

라이브 테스트는 짧은 문장 하나를 임베딩하고 1536차원 벡터가 반환되는지 확인하므로 실제 API 비용이 소량 발생합니다. `OPENAI_API_KEY`가 없으면 태스크를 건너뛰며, 일반 `:apps:backend:test`에서는 `openai-live` 태그를 항상 제외합니다.

### 문서를 pgvector에 색인

M4.5 색인은 기본적으로 비활성화되어 있습니다. PostgreSQL을 시작하고 아래 환경 변수를 설정한 터미널에서 백엔드를 실행합니다.

```powershell
docker compose up -d postgres
$env:OPENAI_API_KEY = "발급받은 API 키"
$env:RAG_PERSISTENCE_ENABLED = "true"
$env:RAG_INDEXING_ENABLED = "true"
./gradlew.bat :apps:backend:bootRun
```

먼저 비용이 발생하지 않는 미리보기를 실행합니다. `embeddedChunkCount`와 `embeddingCharacterCount`가 실제 OpenAI 전송 예상량입니다.

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/rag/index"
```

예상량을 확인한 뒤에만 실제 색인을 명시적으로 실행합니다.

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/rag/index?dryRun=false"
```

기존 문서의 같은 본문 해시와 모델로 저장된 벡터는 재사용하고 새 청크만 OpenAI에 요청합니다. 기본 방어 한도는 문서 200개, 문서당 청크 200개, 실행당 신규 임베딩 입력 500,000자이며 각각 `RAG_INDEXING_MAX_DOCUMENTS`, `RAG_INDEXING_MAX_CHUNKS_PER_DOCUMENT`, `RAG_INDEXING_MAX_CHARACTERS_PER_RUN`으로 더 낮출 수 있습니다. 한도를 넘으면 OpenAI 호출 전에 요청을 중단하고, 동시에 두 색인을 실행하는 것도 차단합니다.

프론트엔드 사이드바의 `문서 색인` 화면에서도 같은 작업을 수행할 수 있습니다. `색인 상태 확인`은 OpenAI를 호출하지 않는 dry-run이며 신규·수정·삭제 문서, 신규 임베딩 Chunk, 재사용 Chunk와 입력 문자 수를 문서별로 표시합니다. 내용을 확인하고 `변경 사항 색인`을 누른 경우에만 확인 창을 거쳐 실제 색인을 실행합니다. 현재 문서의 제목·경로·태그·Chunk 해시·섹션 구조·임베딩 모델이 저장 상태와 모두 같으면 OpenAI 호출뿐 아니라 PostgreSQL 문서 교체도 건너뜁니다.

저장 결과는 Docker의 PostgreSQL 안 `document_chunk` 테이블에서 확인할 수 있습니다.

```powershell
docker compose exec postgres psql -U cs_notes -d cs_notes -c "SELECT document_title, count(*) AS chunks, embedding_model, max(indexed_at) AS indexed_at FROM document_chunk GROUP BY document_title, embedding_model ORDER BY document_title;"
```

IntelliJ Database 또는 DBeaver에서는 `localhost:5432`, 데이터베이스·사용자·비밀번호 `cs_notes`로 연결한 뒤 `public.document_chunk` 테이블을 열면 됩니다. 실제 벡터는 `embedding` 열에 저장됩니다.

### pgvector 의미 검색 확인

M4.6 검색 API를 사용하려면 색인을 완료한 뒤 백엔드 실행 환경에 검색 기능을 추가로 활성화합니다.

```powershell
$env:RAG_SEARCH_ENABLED = "true"
./gradlew.bat :apps:backend:bootRun
```

검색 요청 한 번마다 검색어 임베딩이 필요하므로 유료 호출임을 드러내기 위해 POST API로 제공합니다.

```powershell
$body = @{
  query = "Spring 트랜잭션 전파는 어떻게 동작하나요?"
  limit = 5
  minimumScore = 0.5
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/rag/search" `
  -ContentType "application/json" `
  -Body $body
```

결과의 `score`는 pgvector cosine 유사도이며 높을수록 질의와 가까운 Chunk입니다. `documentPath`, `sectionPath`, `content`를 함께 비교해 상위 결과가 실제 질문 의도와 맞는지 확인합니다. 같은 서버 프로세스에서 같은 검색어를 10분 안에 다시 요청하면 질의 벡터를 캐시하며 응답의 `cachedQueryEmbedding`이 `true`가 됩니다.

이미 색인된 로컬 DB를 대상으로 OpenAI 질의 임베딩부터 pgvector 정렬까지 한 번에 확인하려면 다음 라이브 테스트를 사용할 수 있습니다. 실제 API 비용이 소량 발생하며 PostgreSQL 컨테이너가 실행 중이어야 합니다.

```powershell
$env:OPENAI_API_KEY = "발급받은 API 키"
docker compose up -d postgres
./gradlew.bat :apps:backend:ragSearchLiveTest
```

프론트엔드의 문서 목록 검색창 위에서 `일반 검색`과 `의미 검색`을 전환할 수 있습니다. 의미 검색은 검색 버튼 또는 Enter를 눌렀을 때만 실행하며, 결과를 문서별로 묶어 최고 관련도, 일치한 제목 계층, Chunk 미리보기와 원문 링크를 표시합니다. 화면에서 사용하려면 백엔드를 시작하기 전에 `OPENAI_API_KEY`, `RAG_PERSISTENCE_ENABLED=true`, `RAG_SEARCH_ENABLED=true`를 설정하고 M4.5 색인을 먼저 완료해야 합니다.

### 문서 기반 RAG 답변 생성

M4.8 답변 기능은 검색 결과를 참고 자료로 구성한 뒤 OpenAI 채팅 모델을 호출합니다. 기본적으로 비활성화되어 있으므로 백엔드 실행 전에 다음 값을 추가합니다.

```powershell
$env:OPENAI_API_KEY = "발급받은 API 키"
$env:RAG_PERSISTENCE_ENABLED = "true"
$env:RAG_SEARCH_ENABLED = "true"
$env:RAG_ANSWER_ENABLED = "true"
./gradlew.bat :apps:backend:bootRun
```

프론트엔드 사이드바의 `문서에 질문`에서 질문을 제출하면 Markdown 답변과 `[1]` 형식의 출처 문서가 함께 표시됩니다. 관련 Chunk가 없으면 채팅 모델을 호출하지 않으며, 기본적으로 출처 4개, 컨텍스트 12,000자, 출력 600토큰으로 제한합니다. 같은 질문과 Chunk 조합의 답변은 10분간 캐시합니다. 모델과 한도는 `RAG_ANSWER_MODEL`, `RAG_ANSWER_MAX_OUTPUT_TOKENS`, `RAG_ANSWER_MAX_CONTEXT_CHARACTERS`, `RAG_ANSWER_DEFAULT_SOURCE_LIMIT`으로 조정할 수 있습니다.

실제 OpenAI와 이미 색인된 pgvector를 함께 검증하려면 PostgreSQL 실행 후 별도 라이브 테스트를 사용합니다. 검색어 임베딩과 답변 생성 비용이 발생합니다.

```powershell
docker compose up -d postgres
$env:OPENAI_API_KEY = "발급받은 API 키"
./gradlew.bat :apps:backend:ragAnswerLiveTest
```

OpenAI의 텍스트 생성 지침 구성은 [공식 OpenAI 텍스트 생성 문서](https://developers.openai.com/api/docs/guides/text)를 참고했습니다.

### RAG 답변 비용과 실행 기록

M4.9부터 답변 요청의 상태, 토큰 수, 예상 비용, 소요 시간과 출처 수를 PostgreSQL의 `rag_answer_usage`에 기록합니다. 질문 원문은 저장하지 않고 SHA-256 해시만 보관합니다. 화면에는 요청 ID 앞 8자리와 해당 응답의 예상 비용을 표시합니다.

기본 일일 예상 비용 한도는 `$0.25`입니다. 오늘 누적 비용과 다음 호출의 보수적인 최대 예상 비용을 합산해 한도를 넘으면 OpenAI 호출 전에 `429`로 차단합니다. 가격이나 한도는 모델 가격 변경과 사용 목적에 맞게 직접 조정해야 합니다.

```powershell
$env:RAG_ANSWER_DAILY_COST_LIMIT_USD = "0.25"
$env:RAG_ANSWER_INPUT_PRICE_PER_MILLION_USD = "0.40"
$env:RAG_ANSWER_OUTPUT_PRICE_PER_MILLION_USD = "1.60"
$env:RAG_ANSWER_BUDGET_ZONE_ID = "Asia/Seoul"
```

오늘 실행 기록은 PostgreSQL에서 다음과 같이 확인할 수 있습니다.

```sql
SELECT request_id, status, model, prompt_tokens, completion_tokens,
       estimated_cost_usd, source_count, elapsed_ms, created_at
FROM rag_answer_usage
WHERE created_at >= CURRENT_DATE
ORDER BY created_at DESC;
```

`estimated_cost_usd`는 설정한 토큰 단가로 계산한 참고값이며 최종 청구 금액은 OpenAI 사용량 대시보드가 기준입니다. 캐시 응답과 검색 근거가 없는 응답은 답변 모델 비용을 `$0`으로 기록합니다. 실패 응답은 서버가 실제 처리했을 가능성까지 고려해 보수적인 최대 예상 비용으로 기록합니다.

## 목차 (Categories)

### [백엔드 (Backend)](./백엔드)
#### [Spring](./백엔드/Spring)
*   [의존성 주입(DI)](./백엔드/Spring/의존성%20주입(DI).md)
*   [Spring Bean 관리](./백엔드/Spring/Spring%20Bean%20관리.md)
*   [@Value 어노테이션 주의점](./백엔드/Spring/@Value%20어노테이션%20주의점.md)
*   [Spring MVC 실행 흐름](./백엔드/Spring/Spring%20MVC%20실행%20흐름.md)
*   [@ResponseBody 동작 방식](./백엔드/Spring/@ResponseBody%20동작%20방식.md)
*   [Filter vs Interceptor](./백엔드/Spring/Filter%20vs%20Interceptor.md)
*   [@ExceptionHandler 어노테이션](./백엔드/Spring/@ExceptionHandler%20어노테이션.md)
*   [스프링 트랜잭션 전파 속성](./백엔드/Spring/스프링%20트랜잭션%20전파%20속성.md)
*   [스프링 트랜잭션 AOP 동작 흐름](./백엔드/Spring/스프링%20트랜잭션%20AOP%20동작%20흐름.md)
*   [트랜잭션 롤백 예외](./백엔드/Spring/트랜잭션%20롤백%20예외.md)
*   [JPA 페이징 쿼리](./백엔드/Spring/JPA%20페이징%20쿼리.md)
*   [JPA Fetch Join과 페이징](./백엔드/Spring/JPA%20Fetch%20Join과%20페이징.md)
*   [Lazy Loading](./백엔드/Spring/Lazy%20Loading.md)
*   [OneToOne 관계 Lazy Loading](./백엔드/Spring/OneToOne%20관계%20Lazy%20Loading.md)
*   [Statement vs PreparedStatement](./백엔드/Spring/Statement%20vs%20PreparedStatement.md)

#### [보안 (Security)](./백엔드/보안)
*   [쿠키 vs 세션](./백엔드/보안/쿠키%20vs%20세션.md)
*   [JWT 특징 및 주의사항](./백엔드/보안/JWT%20특징%20및%20주의사항.md)
*   [암호화 방식(대칭키-비대칭키)](./백엔드/보안/암호화%20방식(대칭키-비대칭키).md)
*   [CSRF 공격](./백엔드/보안/CSRF%20공격.md)

#### [DevOps](./백엔드/DevOps)
*   [IaC(Infrastructure as Code)](./백엔드/DevOps/IaC(Infrastructure%20as%20Code).md)
*   [Gradle](./백엔드/DevOps/Gradle.md)
*   [서버리스](./백엔드/DevOps/서버리스.md)

---

### [소프트웨어 공학 (Software Engineering)](./소프트웨어%20공학)
*   [Ack와 메시지 유실 방지 전략](./소프트웨어%20공학/Ack와%20메시지%20유실%20방지%20전략.md)
*   [가상화](./소프트웨어%20공학/가상화.md)
*   [무중단 배포](./소프트웨어%20공학/무중단%20배포.md)
*   [SOLID 원칙](./소프트웨어%20공학/SOLID%20원칙.md)
*   [레이어드 아키텍처](./소프트웨어%20공학/레이어드%20아키텍처.md)
*   [명령어 파이프라인](./소프트웨어%20공학/명령어%20파이프라인.md)
*   [참조 지역성의 원리](./소프트웨어%20공학/참조%20지역성의%20원리.md)
*   [CI-CD 파이프라인](./소프트웨어%20공학/CI-CD%20파이프라인.md)
*   [단일 장애 지점(SPOF)](./소프트웨어%20공학/단일%20장애%20지점(SPOF).md)

#### [테스트 (Test)](./소프트웨어%20공학/테스트)
*   [테스트 격리](./소프트웨어%20공학/테스트/테스트%20격리.md)
*   [테스트 더블](./소프트웨어%20공학/테스트/테스트%20더블.md)
*   [테스트 주도 개발(TDD)](./소프트웨어%20공학/테스트/테스트%20주도%20개발(TDD).md)

#### [디자인 패턴 (Design Pattern)](./소프트웨어%20공학/디자인%20패턴)
*   [전략 패턴](./소프트웨어%20공학/디자인%20패턴/전략%20패턴.md)
*   [PRG 패턴](./소프트웨어%20공학/디자인%20패턴/PRG%20패턴.md)
*   [싱글톤 패턴](./소프트웨어%20공학/디자인%20패턴/싱글톤%20패턴.md)
*   [CQRS 패턴](./소프트웨어%20공학/디자인%20패턴/CQRS%20패턴.md)
*   [템플릿 메서드 패턴](./소프트웨어%20공학/디자인%20패턴/템플릿%20메서드%20패턴.md)
*   [트랜잭셔널 아웃박스 패턴](./소프트웨어%20공학/디자인%20패턴/트랜잭셔널%20아웃박스%20패턴.md)
*   [널 오브젝트 패턴](./소프트웨어%20공학/디자인%20패턴/널%20오브젝트%20패턴(Null%20Object%20Pattern).md)

#### [관측성 (Observability)](./소프트웨어%20공학/관측성)
*   [헬스 체크](./소프트웨어%20공학/관측성/헬스%20체크.md)
*   [Micrometer](./소프트웨어%20공학/관측성/Micrometer.md)
*   [K6-Prometheus-Grafana](./소프트웨어%20공학/관측성/K6-Prometheus-Grafana.md)

---

### [데이터베이스 (Database)](./데이터베이스)
*   [데이터베이스 정규화](./데이터베이스/데이터베이스%20정규화.md)
*   [SQL 인젝션](./데이터베이스/SQL%20인젝션.md)
*   [최종적 일관성](./데이터베이스/최종적%20일관성.md)
*   [이벤트 소싱](./데이터베이스/이벤트%20소싱.md)
*   [트랜잭션 격리 수준](./데이터베이스/트랜잭션%20격리%20수준.md)
*   [Lock(낙관적-비관적)](./데이터베이스/Lock(낙관적-비관적).md)
*   [분산환경 Redis 잠금](./데이터베이스/분산환경%20Redis%20잠금.md)
*   [논리 vs 물리 삭제](./데이터베이스/논리%20vs%20물리%20삭제.md)
*   [RDB 페이징 쿼리 필요성](./데이터베이스/RDB%20페이징%20쿼리%20필요성.md)
*   [NoSQL 데이터베이스 유형](./데이터베이스/NoSQL%20데이터베이스%20유형.md)
*   [DB 차이점(행 기반, 열 기반)](./데이터베이스/DB%20차이점(행%20기반,%20열%20기반).md)
*   [NOT IN 쿼리 문제 및 최적화](./데이터베이스/NOT%20IN%20쿼리%20문제%20및%20최적화.md)

---

### [운영체제 (Operating System)](./운영체제)
*   [시스템 콜](./운영체제/시스템%20콜.md)
*   [프로세스 시스템(단일-멀티)](./운영체제/프로세스%20시스템(단일-멀티).md)
*   [멀티 스레딩](./운영체제/멀티%20스레딩.md)
*   [스레드 vs 코루틴](./운영체제/스레드%20vs%20코루틴.md)
*   [멀티 태스킹의 한계](./운영체제/멀티%20태스킹의%20한계.md)
*   [RAID 기술](./운영체제/RAID%20기술.md)
*   [연속 메모리 할당 기법](./운영체제/연속%20메모리%20할당%20기법.md)
*   [페이지 교체 알고리즘](./운영체제/페이지%20교체%20알고리즘.md)
*   [동시성 문제 중 경쟁 상태 해결](./운영체제/동시성%20문제%20중%20경쟁%20상태%20해결.md)
*   [우아한 종료(Graceful Shutdown)](./운영체제/우아한%20종료(Graceful%20Shutdown).md)

### [프로그래밍 (Programming)](./프로그래밍)
*   [객체 지향 프로그래밍(OOP)](./프로그래밍/객체%20지향%20프로그래밍(OOP).md)
*   [함수형 프로그래밍](./프로그래밍/함수형%20프로그래밍.md)
*   [자바 프로그램 실행 흐름](./프로그래밍/자바%20프로그램%20실행%20흐름.md)
*   [클래스 정보(JAVA)](./프로그래밍/클래스%20정보(JAVA).md)
*   [자바 제네릭(공변-반공변-무공변)](./프로그래밍/자바%20제네릭(공변-반공변-무공변).md)
*   [GC 알고리즘](./프로그래밍/GC%20알고리즘.md)
*   [Thread-Safe](./프로그래밍/Thread-Safe.md)
*   [ThreadLocal](./프로그래밍/ThreadLocal.md)
*   [Thread Pool 포화 정책](./프로그래밍/Thread%20Pool%20포화%20정책.md)
*   [String 객체](./프로그래밍/String%20객체.md)
*   [자바 String 변환(Casting vs valueOf)](./프로그래밍/자바%20String%20변환(Casting%20vs%20valueOf).md)
*   [JCF 초기 용량 설정](./프로그래밍/JCF%20초기%20용량%20설정.md)
*   [try-with-resources](./프로그래밍/try-with-resources.md)

### [네트워크 (Network)](./네트워크)
*   [DNS(Domain Name System)](./네트워크/DNS(Domain%20Name%20System).md)
*   [CDN(Content Delivery Network)](./네트워크/CDN(Content%20Delivery%20Network).md)
*   [NAT 기능](./네트워크/NAT%20기능.md)
*   [클래스풀 IP 주소 체계](./네트워크/클래스풀%20IP%20주소%20체계.md)
*   [정적 및 동적 IP 주소 할당](./네트워크/정적%20및%20동적%20IP%20주소%20할당.md)
*   [Keep-Alive](./네트워크/Keep-Alive.md)
*   [교환 방식(회선-패킷)](./네트워크/교환%20방식(회선-패킷).md)

### [자료구조 (Data Structure)](./자료구조)
*   [연결 리스트(Linked List)](./자료구조/연결%20리스트(Linked%20List).md)
*   [이진 트리](./자료구조/이진%20트리.md)
*   [트라이(Trie)](./자료구조/트라이(Trie).md)
*   [시간-공간 복잡도](./자료구조/시간-공간%20복잡도.md)

### [AI](./AI)
*   [RAG(검색 증강 생성)](./AI/RAG(검색%20증강%20생성).md)
*   [MCP(Model Context Protocol)](./AI/MCP(Model%20Context%20Protocol).md)
*   [LLM 토큰(token)](./AI/LLM%20토큰(token).md)
*   [함수 호출(Tool Use)](./AI/함수%20호출(Tool%20Use).md)
