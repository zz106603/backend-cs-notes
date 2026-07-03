### Lazy Loading (지연 로딩)

**정의**
- 객체를 조회할 때, 연관된 객체(데이터)를 **즉시 가져오지 않고, 실제로 사용할 때 가져오는 기법**
- 반대 개념은 **즉시 로딩 (Eager Loading)** 으로, 객체를 조회할 때 연관된 데이터까지 한 번에 다 가져오는 방식

**비유**
- **Eager Loading:** 마트에서 장을 볼 때, 당장 안 먹을 과자까지 몽땅 카트에 담아서 계산하는 것 (무겁고 느림)
- **Lazy Loading:** 일단 필요한 쌀만 사고, 과자는 나중에 먹고 싶을 때 다시 가서 사오는 것 (가볍고 빠름)

---

### 동작 원리: 프록시 (Proxy)

JPA는 지연 로딩을 구현하기 위해 **프록시(Proxy)** 라는 가짜 객체를 사용함

1.  **가짜 객체 주입:**
    - `Member`를 조회할 때 `Team`이 지연 로딩으로 설정되어 있다면, JPA는 실제 `Team` 객체 대신 **프록시 객체(껍데기)** 를 `Member` 안에 넣어둠
    - 이 프록시 객체는 실제 클래스를 상속받아 만들어지므로 겉모습은 똑같음
2.  **초기화 (Initialization):**
    - 개발자가 `member.getTeam().getName()` 처럼 **실제 데이터를 사용하는 시점**에, 프록시 객체가 영속성 컨텍스트에 요청을 보냄
    - 영속성 컨텍스트는 DB에 쿼리를 날려 실제 `Team` 데이터를 가져와 프록시 객체에 연결(Target 설정)해줌

```java
// 1. Member만 조회 (Team은 프록시 상태)
Member member = em.find(Member.class, 1L); 

// 2. Team 객체 자체는 프록시임
Team team = member.getTeam(); 
System.out.println(team.getClass()); // class hello.jpa.Team$HibernateProxy...

// 3. 실제 사용 시점에 DB 쿼리 나감 (초기화)
System.out.println(team.getName()); 
```

---

### 장점과 단점

**장점**
- **초기 로딩 속도 향상:** 당장 필요 없는 데이터를 안 가져오므로, 첫 화면 로딩이 빠름
- **메모리 절약:** 불필요한 객체를 메모리에 올리지 않음

**단점**
- **지연된 쿼리 비용:** 나중에 데이터를 쓸 때마다 추가 쿼리가 발생하므로, 네트워크 통신 횟수가 늘어날 수 있음
- **N+1 문제 발생 가능성:** (아래에서 설명)

---

### 실무 주의사항: N+1 문제

지연 로딩을 쓰면 필연적으로 겪게 되는 가장 유명한 성능 문제

**상황**
- `Member` 10명을 조회 (쿼리 1번)
- 루프를 돌면서 각 `Member`의 `Team` 이름을 출력
- 이때 각 `Member`마다 `Team`을 조회하는 쿼리가 추가로 나감 (쿼리 N번)
- **총 쿼리 수: 1 + N (10) = 11번**

**해결 방법**
1.  **Fetch Join:** 처음부터 `join fetch`를 써서 한 방 쿼리로 다 가져옴 (가장 많이 사용)
    - `select m from Member m join fetch m.team`
2.  **@EntityGraph:** 어노테이션으로 Fetch Join과 비슷한 효과를 냄
3.  **Batch Size:** `hibernate.default_batch_fetch_size` 옵션을 켜서, N번 나갈 쿼리를 `IN` 절로 묶어서 1번(또는 몇 번)으로 줄임

---

### 결론: 실무 가이드

- **기본 전략:** 모든 연관 관계는 **지연 로딩(LAZY)** 으로 설정 (`@ManyToOne`, `@OneToOne`은 기본이 EAGER이므로 꼭 LAZY로 변경)
- **최적화:** 개발하다가 N+1 문제가 발생하거나, 한 번에 같이 가져오는 게 확실히 유리한 곳에만 **Fetch Join**을 적용
- **즉시 로딩(EAGER)은 상상 이상으로 예측 불가능한 쿼리를 만들어내므로 절대 쓰지 말기**