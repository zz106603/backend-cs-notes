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
