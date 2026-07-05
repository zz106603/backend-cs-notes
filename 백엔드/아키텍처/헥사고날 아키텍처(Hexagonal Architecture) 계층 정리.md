# Hexagonal Architecture 계층 정리

Hexagonal Architecture의 목적은 **비즈니스 로직을 외부 기술(DB, MQ, HTTP, Framework)으로부터 분리**하는 것

구조를 단순화하면 다음과 같음

```text id="5ovv8q"
External Systems (DB / MQ / API)
          ↓
        Infra
          ↓
       UseCase
          ↓
        Domain
```

안쪽 계층일수록 비즈니스 중심, 바깥 계층일수록 기술 중심

---

## 계층별 역할

| Layer   | 역할         | 핵심 질문      | 예시                          |
| ------- | ---------- | ---------- | --------------------------- |
| Domain  | 핵심 비즈니스 규칙 | 무엇인가?      | OutboxMessage, OutboxStatus |
| UseCase | 업무 흐름 조립   | 어떻게 동작하는가? | OutboxPoller, RetryOutbox   |
| Infra   | 외부 기술 연결   | 무엇과 연결되는가? | RabbitMQ, JPA, Redis        |
| App     | 실행/설정      | 어떻게 띄우는가?  | Config, Scheduler           |
| Common  | 공통 유틸      | 공통으로 필요한가? | Exception, Utils            |

---

## Domain

Domain은 시스템의 핵심 개념과 상태 전이 규칙을 표현함

예를 들어 Outbox 시스템에서 다음은 Domain에 가까움

```text id="h4nwhk"
OutboxMessage
OutboxStatus
DeadLetterReasonCode
```

특징:

* 기술 의존성 최소화
* Spring/JPA/MQ 의존 제거
* 시스템의 핵심 규칙 표현

---

## UseCase

UseCase는 실제 업무 시나리오를 조립함

예시:

```text id="hqqrjh"
PENDING 조회
→ PROCESSING claim
→ publish
→ 성공/실패 처리
```

단순 CRUD가 아니라 **업무 흐름 orchestration** 이 핵심

대표 예시:

```text id="2j7grf"
OutboxPoller
```

---

## Infra

Infra는 외부 시스템과의 입출력을 담당함

예:

```text id="fgq2fd"
JpaOutboxRepository
RabbitMqPublisher
ConsumerListener
GoogleApiClient
```

기술 어노테이션이 보이면 대부분 Infra일 가능성이 높음

```java id="b3z2zv"
@RabbitListener
@Repository
@Entity
```

---

## ConsumerListener vs OutboxPoller

이 둘이 가장 헷갈리기 쉬움

### ConsumerListener → Infra

역할:

```text id="dg4j0r"
메시지 수신
→ payload parsing
→ usecase 호출
```

RabbitMQ 의존성이 강함

---

### OutboxPoller → UseCase

역할:

```text id="js6od0"
PENDING 조회
→ claim
→ publish
→ 상태 변경
```

Outbox 발행 시나리오 전체를 관리함

---

## Policy는 어디에 둘까?

Policy는 이름만 보고 판단하면 안 됨

### Domain Policy

비즈니스 규칙:

```text id="yvdfg0"
DiscountPolicy
PricingPolicy
AuthorizationPolicy
```

→ Domain

---

### UseCase Policy

운영 정책:

```text id="9lr6gl"
RetryPolicy
BackoffPolicy
TimeoutPolicy
```

예:

```text id="gtokgk"
1차 실패 → 10초 retry
2차 실패 → 30초 retry
3차 실패 → DLQ
```

→ UseCase 또는 Infra

---

## 판단 기준

패키지 위치가 헷갈릴 때는 아래 순서로 판단함

### 1. 외부 기술이 바뀌어도 살아남는가?

* YES → Domain / UseCase
* NO → Infra

예:

```text id="gkhqfh"
RabbitMQ → Kafka 변경
```

* ConsumerListener → 변경 필요 → Infra
* Outbox flow → 유지 → UseCase

---

### 2. 비즈니스 규칙인가 운영 흐름인가?

* 비즈니스 규칙 → Domain
* 업무 흐름 → UseCase

---

## 핵심 정리

```text id="d5qg1j"
Domain  → What
UseCase → How
Infra   → With What
```

패키지 이름보다 중요한 것은 **책임(Responsibility)** 임
