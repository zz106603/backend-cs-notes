## CQRS 패턴 (Command Query Responsibility Segregation)

### 1. 개념
**명령(Command)과 조회(Query)의 책임을 분리하는 패턴**
- **Command (명령)**: 시스템의 상태를 변경하는 작업 (Create, Update, Delete)
- **Query (조회)**: 시스템의 상태를 반환하는 작업 (Read)

일반적인 모델은 하나의 객체가 명령과 조회를 모두 처리하지만, CQRS는 이를 물리적 또는 논리적으로 분리하여 각자의 역할에 최적화된 설계를 가능하게 함

---

### 2. 왜 사용하는가? (등장 배경)
전통적인 CRUD 아키텍처에서는 하나의 모델(Entity)로 데이터 조회와 업데이트를 모두 처리. 하지만 서비스가 복잡해지면 다음과 같은 문제가 발생

1.  **모델의 불일치**: 데이터를 저장할 때 필요한 필드와 화면에 보여줄 때 필요한 필드가 다름 (예: 조회 시에는 여러 테이블의 조인이 필요함)
2.  **성능 최적화의 어려움**: 쓰기 작업은 트랜잭션과 정합성이 중요하고, 읽기 작업은 빠른 속도가 중요함. 하나의 모델로는 두 마리 토끼를 잡기 어려움

---

### 3. CQRS 적용 단계 (예시)

#### 1단계: 코드 레벨의 분리 (가장 단순한 형태)
동일한 데이터베이스를 사용하되, **모델(DTO/Entity)만 분리**하는 방식
- **Command**: JPA의 Entity를 사용하여 도메인 로직을 처리하고 상태를 변경
- **Query**: 화면에 뿌려줄 전용 DTO를 만들거나, MyBatis/JdbcTemplate 등을 사용하여 조회 성능을 최적화

```java
// Command Service (상태 변경) - JPA 사용
@Transactional
public void cancelOrder(Long orderId) {
    Order order = orderRepository.findById(orderId);
    order.cancel(); // 도메인 로직 수행
}

// Query Service (조회) - MyBatis 또는 JPQL 직접 사용
public OrderDto getOrderDetails(Long orderId) {
    return queryMapper.selectOrderDetails(orderId); // 복잡한 조인 쿼리 최적화
}
```

#### 2단계: 데이터베이스의 분리 (고급 형태)
**쓰기 전용 DB(Master)** 와 **읽기 전용 DB(Slave/Replica)** 를 물리적으로 분리하거나, 아예 다른 종류의 저장소를 사용하는 방식

- **Command DB**: 데이터 정합성이 중요한 RDBMS (MySQL, PostgreSQL) 사용
- **Query DB**: 조회 성능이 뛰어난 NoSQL (MongoDB, Redis, Elasticsearch) 사용
- **동기화**: Command DB에 변경이 발생하면, 이벤트(Event Sourcing)나 메시지 큐(Kafka)를 통해 Query DB로 데이터를 전파하여 동기화 (Eventual Consistency - 결과적 일관성)

---

### 4. 장점과 단점

#### -- 장점 --
1.  **최적화**: 명령과 조회 각각에 맞는 최적의 기술과 설계를 적용할 수 있음 (예: 조회용 쿼리 튜닝, 캐싱 적용 용이)
2.  **단순화**: 비즈니스 로직(명령)과 화면 출력 로직(조회)이 섞이지 않아 코드가 깔끔해지고 유지보수가 쉬워짐
3.  **확장성**: 조회 요청이 훨씬 많은 경우, 조회 전용 서버나 DB만 스케일 아웃(Scale-out)하여 성능을 높일 수 있음

#### -- 단점 --
1.  **복잡도 증가**: 구현해야 할 코드 양이 늘어나고, 아키텍처가 복잡해짐
2.  **데이터 동기화 문제**: DB를 분리할 경우, 데이터가 실시간으로 일치하지 않을 수 있음 (데이터가 전파되는 시간 차이 발생)

---

### 5. 결론 및 실무 팁
- 무조건 CQRS를 적용하는 것은 좋지 않음. 단순한 CRUD 서비스라면 오히려 오버엔지니어링이 될 수 있음
- **추천 전략**:
  - 처음에는 **코드 레벨(1단계)** 에서 명령과 조회 모델을 분리하는 것부터 시작
  - 트래픽이 많아지거나 조회 로직이 너무 복잡해져서 성능 이슈가 발생할 때, **DB 분리(2단계)** 를 고려하는 것이 좋음