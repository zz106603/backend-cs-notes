### @OneToOne 관계에서 Lazy Loading 이슈

**문제 상황**
- JPA에서 `@OneToOne` 관계를 맺을 때, **연관 관계의 주인이 아닌 쪽(mappedBy가 있는 쪽)** 에서 조회하면 `FetchType.LAZY`로 설정해도 **즉시 로딩(Eager Loading)** 이 발생해버리는 문제
- 이로 인해 의도치 않은 **N+1 문제**가 발생하여 성능 저하의 원인이 됨

---

### 왜 이런 일이 발생하나요? (원인 분석)

핵심은 **"프록시(Proxy) 객체를 만들려면 값이 있는지 없는지 알아야 한다"** 는 점

1.  **프록시의 조건:** JPA는 지연 로딩을 위해 실제 객체 대신 '가짜 객체(프록시)'를 넣어둠. 단, **값이 `null`이면 프록시를 만들지 않고 그냥 `null`을 넣음**
2.  **테이블 구조의 한계:**
    - **연관 관계 주인(FK 보유):** 내 테이블에 외래키(FK)가 있으므로, 그 컬럼만 보면 값이 있는지(`null`인지 아닌지) 바로 알 수 있음 -> **지연 로딩 가능**
    - **주인이 아닌 쪽(FK 없음):** 내 테이블만 봐서는 상대방이 나를 참조하는지 알 방법이 없음. 상대방 테이블을 뒤져봐야(SELECT) 암
3.  **결론:** 주인이 아닌 쪽은 "값이 있는지 확인하기 위해" 어차피 쿼리를 날려야 하므로, 기왕 쿼리 날린 김에 데이터까지 다 가져와버림 (그래서 Eager로 동작)

**예시 코드**
```java
// [주인이 아닌 쪽] - User 테이블에는 account_id 컬럼이 없음
@Entity
public class User {
    @Id 
    private Long id;

    // 나는 FK가 없어서 Account 테이블을 까봐야만 값이 있는지 알 수 있음 -> 강제 Eager행
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Account account;
}

// [주인] - Account 테이블에는 user_id(FK)가 있음
@Entity
public class Account {
    @Id 
    private Long id;

    // 나는 user_id 컬럼만 보면 null인지 아니까 프록시 생성 가능 -> Lazy 잘 됨
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

---

### 해결 방법

#### 1. 단방향 관계 사용 (가장 추천)
- 양방향 연관 관계를 끊고, **FK를 가진 쪽(주인)에서만 조회**하도록 설계를 변경
- 객체지향적으로 조금 불편할 수 있지만, 성능상 가장 깔끔함

#### 2. @OneToOne 대신 @OneToMany 사용
- 비즈니스 로직상 1:1이라도 기술적으로 `List<Account>` 처럼 1:N으로 풀면 지연 로딩이 정상 동작함 (컬렉션은 `PersistentBag`이라는 래퍼를 써서 지연 로딩을 지원함)
- 하지만 1개만 존재한다는 제약 조건을 애플리케이션 레벨에서 관리해야 하는 단점이 있음

#### 3. Bytecode Instrumentation (고급)
- Hibernate의 `hibernate-enhance-maven-plugin` 같은 플러그인을 사용하여, 컴파일 시점에 바이트코드를 조작하는 방식
- 필드에 접근하는 시점에 쿼리를 날리도록 강제할 수 있음 (`@LazyToOne(LazyToOneOption.NO_PROXY)`)
- 설정이 복잡하고 운영 환경 관리가 까다로워 잘 쓰지 않음

#### 4. Fetch Join 사용
- 지연 로딩이 안 된다면, 차라리 처음부터 `Fetch Join`으로 한 방 쿼리로 가져와서 N+1 문제를 예방함
- `SELECT u FROM User u JOIN FETCH u.account`

---

### 요약

| 상황 | Lazy Loading 동작 여부 | 이유 |
| :--- | :--- | :--- |
| **연관 관계 주인 (FK O)** | **O (동작함)** | FK 컬럼만 보면 null 여부 판단 가능 |
| **주인이 아닌 쪽 (FK X)** | **X (동작 안 함)** | 상대 테이블을 조회해봐야 null 여부 판단 가능 |

**실무 팁:** `@OneToOne` 양방향은 가급적 피하고, 꼭 필요하다면 **Fetch Join**을 쓰거나 **단방향**으로 푸는게 나음