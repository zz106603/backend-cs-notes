# Spring에서 객체를 Bean으로 관리하는 이유

Spring 프레임워크가 객체(Bean)의 생성부터 소멸까지의 생명주기를 직접 관리함으로써 얻을 수 있는 기술적 이점과 실무적 가치에 대해 설명

---

## 1. 의존성 주입 (DI) 및 결합도 감소
가장 큰 이유는 **객체 간의 의존성을 Spring 컨테이너가 자동으로 해결**

*   **설명**: 개발자가 직접 `new` 키워드로 객체를 생성하고 의존 관계를 맺어줄 필요가 없음
*   **장점**:
    *   객체 간의 결합도(Coupling)가 낮아져 유지보수가 쉬워짐
    *   순환 참조(Circular Reference) 문제를 애플리케이션 구동 시점에 파악하여 방지할 수 있음

```java
@Service
class OrderService {
    // 개발자가 직접 new ProductRepository()를 하지 않음
    // Spring이 미리 생성해둔 bean을 주입해줌
    private final ProductRepository productRepository;

    public OrderService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

## 2. 싱글톤 (Singleton) 레지스트리 관리
대규모 트래픽 처리가 필요한 엔터프라이즈 환경에서 **메모리 자원을 효율적으로 사용**

*   **설명**: Spring 컨테이너는 기본적으로 빈을 **싱글톤(Singleton)** 스코프로 관리함. 즉, 애플리케이션 전체에서 단 하나의 객체 인스턴스만 생성하여 공유함
*   **실무 효과**:
    *   사용자의 요청이 올 때마다 `Service`, `Repository` 객체를 새로 생성한다면 메모리 낭비가 심하고 GC(Garbage Collection) 부하가 발생함
    *   빈으로 관리하면 1개의 인스턴스를 수만 명의 사용자가 공유하여 사용하므로 성능상 매우 유리함

## 3. AOP (관점 지향 프로그래밍) 적용 가능
**트랜잭션(@Transactional)**, 로깅, 보안 등 공통 관심사를 비즈니스 로직에서 분리하여 적용할 수 있음

*   **원리**: Spring AOP는 **프록시(Proxy)** 패턴을 기반으로 동작함. 빈으로 등록된 객체여야만 Spring이 프록시 객체를 감싸서 트랜잭션 시작/종료 코드를 자동으로 삽입할 수 있음
*   **주의**: `new`로 직접 생성한 객체에는 `@Transactional`을 붙여도 트랜잭션이 동작하지 않음 (Spring 컨테이너의 관리를 받지 못하기 때문)

```java
@Service
class TransferService {
    // 이 메서드가 실행될 때 트랜잭션을 시작하고, 종료 시 커밋/롤백하는 코드가
    // 프록시 객체에 의해 자동으로 실행됨 (Bean이기 때문에 가능)
    @Transactional
    public void transfer(String fromId, String toId, long amount) {
        // 비즈니스 로직만 집중 가능
    }
}
```

## 4. 생명주기 (Lifecycle) 콜백 지원
객체의 생성과 소멸 시점을 Spring이 관리하므로, **리소스의 초기화 및 해제 작업**을 안전하게 수행할 수 있음

*   **실무 예시**: 데이터베이스 커넥션 풀(DBCP) 초기화
    *   애플리케이션 시작 시점(`@PostConstruct`): DB 연결을 미리 맺어둠
    *   애플리케이션 종료 시점(`@PreDestroy`): 연결을 안전하게 종료함

```java
@Component
class DatabaseManager {
    @PostConstruct
    public void init() {
        System.out.println("DB 연결 초기화 완료");
    }

    @PreDestroy
    public void close() {
        System.out.println("DB 연결 안전하게 종료");
    }
}
```

## 5. 설정의 중앙화 및 일관성
애플리케이션 전반에 걸친 설정을 한 곳에서 관리하여 일관성을 유지할 수 있음

*   **예시**: `DataSource`, `ObjectMapper` (JSON 처리기) 등 공통 인프라 빈 설정
*   개발자가 필요할 때마다 `new ObjectMapper()`를 호출하면 설정이 제각각일 수 있지만, 빈으로 등록해두면 모든 곳에서 동일한 설정을 가진 객체를 주입받아 사용할 수 있음

### 요약
> **Spring Bean 관리는 단순한 객체 생성을 넘어, 효율적인 리소스 사용(싱글톤), 선언적 트랜잭션 관리(AOP), 그리고 안전한 생명주기 관리를 가능하게 하는 핵심 메커니즘**