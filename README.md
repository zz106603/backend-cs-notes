# Backend CS Notes

백엔드 개발자를 위한 컴퓨터 공학(CS) 및 소프트웨어 공학 지식 정리 저장소입니다. 실무 예시와 함께 핵심 개념을 정리하고 있습니다.

## 카테고리 (Categories)

### [소프트웨어 공학 (Software Engineering)](./소프트웨어%20공학)
*   **아키텍처 및 원리**: 레이어드 아키텍처, 명령어 파이프라인, 참조 지역성의 원리, 가상화
*   **디자인 패턴**: 전략 패턴, PRG 패턴, 싱글톤 패턴, CQRS 패턴, 템플릿 메서드 패턴, 트랜잭셔널 아웃박스 패턴, 널 오브젝트 패턴
*   **개발 프로세스 및 테스트**: 테스트 주도 개발(TDD), 테스트 격리, 테스트 더블, 무중단 배포, CI-CD 파이프라인
*   **관측성 (Observability)**: 헬스 체크, Micrometer, K6-Prometheus-Grafana
*   **시스템 가용성**: 단일 장애 지점(SPOF)

### [Spring](./Spring)
*   **핵심 원리**: 의존성 주입(DI), Spring Bean 관리, @Value 어노테이션 주의점
*   **트랜잭션**: 스프링 트랜잭션 전파 속성, 트랜잭션 롤백 예외, 스프링 트랜잭션 AOP 동작 흐름
*   **JPA 및 데이터 접근**: Lazy Loading, OneToOne 관계 Lazy Loading, Fetch Join과 페이징, JPA 페이징 쿼리, Statement vs PreparedStatement
*   **예외 처리**: @ExceptionHandler 어노테이션

### [데이터베이스 (Database)](./데이터베이스)
*   **이론 및 정규화**: 데이터베이스 정규화, 최종적 일관성
*   **성능 및 최적화**: RDB 페이징 쿼리 필요성, NOT IN 쿼리 문제 및 최적화, DB 차이점(행 기반/열 기반)
*   **동시성 및 트랜잭션**: Lock(낙관적/비관적), 분산환경 Redis 잠금, 이벤트 소싱
*   **보안 및 관리**: SQL 인젝션, 논리 vs 물리 삭제, NoSQL 데이터베이스 유형

### [운영체제 (Operating System)](./운영체제)
*   **프로세스 및 스레드**: 프로세스 시스템(단일/멀티), 멀티 스레딩, 스레드 vs 코루틴, 멀티 태스킹의 한계
*   **메모리 및 저장장치**: 연속 메모리 할당 기법, 페이지 교체 알고리즘, RAID 기술
*   **시스템 원리**: 시스템 콜, 동시성 문제 중 경쟁 상태 해결, 우아한 종료(Graceful Shutdown)

### [보안 (Security)](./보안)
*   **인증 및 인가**: 쿠키 vs 세션, JWT 특징 및 주의사항
*   **암호화 및 방어**: 암호화 방식(대칭키/비대칭키), CSRF 공격

### [프로그래밍 (Programming)](./프로그래밍)
*   **Java 언어 심화**: 자바 프로그램 실행 흐름, 클래스 정보, 자바 제네릭(공변/반공변/무공변), try-with-resources
*   **메모리 및 동시성**: GC 알고리즘, Thread-Safe, ThreadLocal, Thread Pool 포화 정책
*   **자료형 및 라이브러리**: String 객체, 자바 String 변환(Casting vs valueOf), JCF 초기 용량 설정
*   **패러다임**: 객체 지향 프로그래밍(OOP), 함수형 프로그래밍

### [네트워크 (Network)](./네트워크)
*   **인프라 및 서비스**: DNS(Domain Name System), CDN(Content Delivery Network), NAT 기능
*   **프로토콜 및 주소 체계**: 클래스풀 IP 주소 체계, 정적 및 동적 IP 주소 할당, Keep-Alive, 교환 방식(회선/패킷)

### [자료구조 (Data Structure)](./자료구조)
*   **기초 및 고급**: 연결 리스트(Linked List), 이진 트리, 트라이(Trie), 시간-공간 복잡도

### [AI](./AI)
*   **LLM 서비스 개발**: RAG(검색 증강 생성), MCP(Model Context Protocol)

### [DevOps](./DevOps)
*   **인프라 자동화**: IaC(Infrastructure as Code), Gradle, 서버리스
