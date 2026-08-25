---
title: "Database 핵심 정리"
tags:
  - "데이터베이스"
  - "Database"
  - "정리"
---
# Database 핵심 정리

## 데이터베이스

백엔드 애플리케이션에서 대부분의 핵심 데이터는 데이터베이스에 저장됨

Spring Boot 애플리케이션의 일반적인 요청 흐름

```
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```
실무에서는 단순히 SQL을 작성할 수 있는 것보다 다음 문제를 이해하는 것이 중요함
- 데이터가 많아졌을 때 왜 조회가 느려지는가?
- Index를 사용하면 왜 빨라지는가?
- 여러 요청이 동시에 같은 데이터를 수정하면 어떻게 되는가?
- 트랜잭션 도중 오류가 발생하면 어떻게 되는가?
- Lock 때문에 요청이 멈추거나 Deadlock이 발생하는 이유는 무엇인가?

따라서 백엔드 개발자는 **Index, Transaction, Isolation Level, Lock, MVCC**를 중심으로 데이터베이스를 이해해야 함

## RDB(Relational Database)
데이터를 **Table 형태로 저장하고 관계를 통해 연결하는 데이터베이스**

### User
| id | name |
| -: | ---- |
|  1 | Kim  |
|  2 | Lee  |
### Order
|  id | user_id | amount |
| --: | ------: | -----: |
| 101 |       1 |  10000 |
| 102 |       1 |  20000 |

`ORDER.user_id`를 이용해 주문이 어떤 사용자의 것인지 연결할 수 있음

대표적인 RDBMS
- MySQL
- PostgreSQL
- Oracle
- MariaDB

## Primary Key와 Foreign Key

### Primary Key
**Table에서 하나의 Row를 고유하게 식별하는 값**

| id | name |
| -: | ---- |
|  1 | Kim  |
|  2 | Lee  |

여기서 `id`가 Primary Key

특징
- 중복될 수 없음
- NULL일 수 없음
- 하나의 ROW를 식별

### Foreign Key
**다른 Table의 데이터를 참조하기 위한 Key**

`ORDER.user_id` -> `USER.id`

이를 통해 데이터 사이의 관계를 표현할 수 있음

## JOIN
**여러 Table의 데이터를 연결해서 조회하는 방법**

예를 들어 사용자와 주문 정보를 함께 조회
```SQL
SELECT u.name, o.amount
FROM users u
JOIN orders o
  ON u.id = o.user_id;
```

### INNER JOIN
양쪽 Table 모두에게 존재하는 데이터만 조회
```
USER ∩ ORDER
```

### LEFT JOIN
왼쪽 Table의 데이터는 모두 조회하고, 오른쪽에 일치하는 값이 없으면 NULL을 반환
```
USER 전체
+
일치하는 ORDER
```

백엔드에서는 복잡한 JOIN 자체보다 JOIN 조건에 적절한 Index가 있는지가 성능에 더 중요한 경우가 많음

## Index
Database에서 가장 중요한 개념 중 하나

Index는 데이터를 빠르게 찾기 위해 만드는 **별도의 검색 구조**

ex) 책에서 특정 단어를 찾을 때 색인을 확인
```
Database Table
1
2
3
4
5
...
1,000,000
```

Index가 없다면 특정 데이터를 찾기 위해 많은 Row를 확인해야 할 수 있음

이를 **Full Table Scan**이라고 함
```
1 → 2 → 3 → 4 → ... → 1,000,000
```

Index가 존재하면 검색 구조를 이용해 필요한 데이터의 위치를 빠르게 찾을 수 있음

## B+Tree Index
MySQL, PostgreSQL 등의 일반적인 Index는 B-Tree 계열 구조를 사용함

실제 DB 구현에서는 **B+Tree 또는 그와 유사한 구조**가 많이 사용됨

단순한 정렬 배열이라고 생각하기보다는 데이터를 여러 단계로 나눠 탐색하는 Tree라고 이해하면 됨

```
              [50]

          /          \

       [20]          [80]

      /   \          /   \
    ...   ...      ...   ...
```

전체 데이터를 하나씩 확인하는 대신 탐색 범위를 계속 줄여나감

탐색 비용을 크게 줄일 수 있음

### 왜 Binary Tree가 아닌가?

Disk 기반 Database에서는 한 번의 Disk I/O 비용이 큼

B+Tree는 **하나의 Node에 여러 Key를 저장해 Tree 높이를 낮춤**
```
Binary Tree

        50
       /  \
     30    70
    /        \
   ...

B+Tree

       [20 40 60 80]
      /   |  |  |   \
```

Tree 높이가 낮아지면 데이터를 찾기 위해 접근해야 하는 페이지 수도 줄어듬

또한 Leaf Node가 순서대로 연결되어 있어 범위 검색에도 유리함
```SQL
WHERE age BETWEEN 20 AND 30
```

## Index의 단점
Index를 만들면 조회 성능은 좋아질 수 있지만 비용도 발생

### 저장 공간
Index 역시 별도의 데이터 구조이므로 저장 공간이 필요함

### INSERT / UPDATE / DELETE 비용
데이터가 변경되면 Index도 함께 수정해야 함
```
Table INSERT
   +
Index INSERT
```
따라서 Index가 지나치게 많으면 쓰기 성능이 저하될 수 있음

즉, **조회가 많다고 모든 Column에 Index를 만드는 것은 좋은 방법이 아님**

## 복합 Index
여러 Column을 하나의 Index로 묶을 수 있음
```SQL
CREATE INDEX idx_user_name_age
ON users(name, age);
```

이 경우 Index는 대략 다음 순서로 정렬되어 있다고 생각할 수 있음

`name -> age`

따라서 Column 순서가 중요함

ex) `(name, age)`
```SQL
WHERE name = 'Kim'

WHERE name = 'Kim'
AND age = 30
```
Index가 있다면 일반적으로 위 조건은 활용하기 좋음

```SQL
WHERE age = 30
```
반면 위만 사용하는 경우에는 해당 복합 Index를 효율적으로 활용하기 어려울 수 있음

이를 이해할 때 중요한 개념이 **Leftmost Prefix**

## Cardinality
**Column 값의 고유한 정도**

예를 들어 성별은 고유 값이 적음
```
M
F
```

주민번호나 사용자 ID는 대부분 값이 다름
```
1
2
3
...
```
일반적으로 Index는 **데이터를 많이 걸러낼 수 있는 Column**에서 효과적

100만명 중 남성 50만명을 찾는 Index보다, 100만건 중 1건을 찾는 Index가 훨씬 효과적

다만 실제 Index 설계는 Cardinality 하나만으로 결정하는 것이 아니라 조회 조건과 데이터 분포를 함께 봐야 함

## 실행 계획
SQL이 느릴 때 무작정 Index만 추가하면 안 됨

Database가 SQL을 **어떤 방식으로 실행하고 있는지** 먼저 확인해야 함

대표적으로 `EXPLANE`을 사용
```SQL
EXPLAIN
SELECT *
FROM users
WHERE email = 'test@test.com';
```

- Table 전체를 읽는지
- Index를 사용하는지
- 어떤 Index를 선택했는지
- JOIN 순서
- 예상 조회 Row 수

### 느린 Query를 만났다면
```
1. 느린 SQL 확인
       ↓
2. 실행 계획 확인
       ↓
3. Full Scan 여부 확인
       ↓
4. Index 사용 여부 확인
       ↓
5. 조회 Row 수 확인
       ↓
6. JOIN / 조건 / Index 개선
       ↓
7. 다시 측정
```
중요한 것은 **Index 추가가 시작점이 아니라 실행계획 확인이 시작점**이라는 것

## Transaction
여러 작업을 **하나의 논리적인 작업 단위로 묶는 것**

```
A 계좌 -10,000원
B 계좌 +10,000원
```
두 작업을 하나의 Transaction으로 처리

```
BEGIN

A -10000
B +10000

COMMIT
```

중간에 문제가 발생하면 `ROLLBACK`

## ACID
**Transaction이 지켜야 하는 대표적인 특성**

### Atomicity - 원자성
Transaction의 작업은 전부 성공하거나 전부 실패해야 함
```
A 출금 성공
B 입금 실패

→ 전체 Rollback
```

### Consistency - 일관성
Transaction 전후에 Database가 정의된 규칙을 만족해야 함
```
잔액은 0원 미만이 될 수 없음
```

### Isolation - 격리성
여러 Transaction이 동시에 실행되어도 서로의 작업이 부적절하게 영향을 주지 않아야 함

### Durability - 지속성
Commit된 데이터는 서버 장애 등이 발생하더라도 보존되어야 함

## 동시성 문제 발생 이유
백엔드 서버에서는 여러 요청이 동시에 들어옴

ex) 상품 재고 1개 남음

두 요청 동시에 조회
```
Request A → 재고 1 확인
Request B → 재고 1 확인
```
둘 다 주문을 진행하면 `재고 = -1`

이런 문제를 맊기 위해 Database에서는
- Transcation
- Isolation Level
- Lock
- MVCC

등을 사용

## Isolation Level
Transaction의 Isolation을 얼마나 강하게 보장할 것인지 정하는 수준
```
READ UNCOMMITTED
        ↓
READ COMMITTED
        ↓
REPEATABLE READ
        ↓
SERIALIZABLE

격리 수준 ↑
동시 처리 성능 ↓
```
격리를 강하게 할수록 데이터 정합성은 높아지지만 동시에 처리할 수 있는 작업이 줄어듬

### Dirty Read
Transaction A가 아직 Commit하지 않은 데이터를 Transaction B가 읽는 문제

`READ COMMITTED` 이상에서는 방지함

### Non-Repeatable Read
같은 Transaction 안에서 같은 Row를 두 번 조회했는데 결과가 달라지는 문제

### Phantom Read
같은 조건으로 조회했는데 없던 Row가 새로 나타나는 현상

### Isolation Level 정리
| Isolation Level  | Dirty Read | Non-Repeatable Read | Phantom Read |
| ---------------- | ---------- | ------------------- | ------------ |
| READ UNCOMMITTED | 발생 가능      | 발생 가능               | 발생 가능        |
| READ COMMITTED   | 방지         | 발생 가능               | 발생 가능        |
| REPEATABLE READ  | 방지         | 방지                  | DB 구현에 따라 다름 |
| SERIALIZABLE     | 방지         | 방지                  | 방지           |

여기서 주의해야 할 점은 실제 동작은 **DBMS의 MVCC 구현 방식에 따라 차이가 있다는 것**

예를 들어 MySQL InnoDB의 REPEATABLE READ는 MVCC와 Next-Key Lock 등을 이용해 표준에서 예상하는 것보다 강하게 Phantom 문제를 제어

## Lock
여러 Transaction이 동시에 같은 데이터에 접근할 때 충돌을 제어하는 방법

### Shared Lock
읽기 Lock

여러 Transaction이 동시에 Shared Lock을 가질 수 있음
```
Transaction A → READ
Transaction B → READ

가능
```

### Exclusive Lock
쓰기 작업을 위한 Lock

Exclusive Lock이 걸린 데이터를 다른 Transaction이 변경하지 못하게 함
```
Transaction A → UPDATE

Transaction B → UPDATE
                 ↓
                대기
```

## 비관적 Lock과 낙관적 Lock

### 비관적 Lock
충돌이 발생할 것이라고 가정하고 먼저 Lock을 검
```SQL
SELECT *
FROM products
WHERE id = 1
FOR UPDATE;
```
```
Transaction A
     ↓
Row Lock
     ↓
수정
     ↓
Commit
```
다른 Transaction은 기다려야 함

장점
- 충돌을 확실하게 방지하기 쉬움

단점
- 대기 증가
- 동시 처리 성능 감소
- Deadlock 가능성

### 낙관적 Lock
충돌이 자주 발생하지 않을 것이라고 가정

보통 Version 값을 이용
```
id = 1
stock = 10
version = 3
```

```SQL
UPDATE product
SET stock = 9,
    version = 4
WHERE id = 1
AND version = 3;
```
다른 Transaction이 먼저 수정했다면 version이 달라져 Update가 실패

**즉, Lock을 먼저 잡는게 아니라 수정 시점에 충돌했는지 확인하는 방식**

JPA의 `@Version`이 대표적

## Deadlock
두 Transaction이 서로가 가진 Lock을 기다리면서 영원히 진행되지 못하는 상황
```
Transaction A
→ Row 1 Lock 획득
→ Row 2 기다림


Transaction B
→ Row 2 Lock 획득
→ Row 1 기다림
```
```
A → B가 가진 Lock 기다림
↑                    ↓
└─ A의 Lock 기다림 ← B
```

### Deadlock 발생 조건

### 1. 상호 배제
한 번에 하나의 작업만 자원을 사용할 수 있음

### 2. 점유와 대기
이미 자원을 가진 상태에서 다른 자원을 기다림

### 3. 비선점
다른 작업이 가진 자원을 강제로 빼앗을 수 없음

### 4. 순환 대기
서로가 서로의 자원을 기다리는 순환 구조가 만들어짐

```
혼자 사용
+
하나는 들고 기다림
+
빼앗을 수 없음
+
서로 기다림
```

### Deadlock을 줄이는 방법
- Lock 획득 순서를 일정하게 유지
- Transaction을 짧게 유지
- 불필요한 Lock 최소화
- 적절한 Index 사용
- Deadlock 발생 시 Transaction 재시도

Database는 Deadlock을 감지하면 보통 Transaction 하나를 강제로 Rollback해서 상황을 해제함

## MVCC
**Multi-Version Concurrency Control**

여러 Transaction이 동시에 데이터를 읽고 수정할 때 Lock 경쟁을 줄이기 위해 데이터의 여러 Version을 관리하는 방식

예를 들어 데이터가 `balance = 100`이었다가 `balance = 200`으로 변경

단순하게 기존 값을 바로 없애는 것이 아니라 개념적으로 여러 Version을 관리할 수 있음
```
Version 1 → balance 100
Version 2 → balance 200
```

Transaction이 언제 시작했는지 등에 따라 적절한 Version을 보여줌

### 왜 필요?
**1. Lock만 사용한다고 가정**
```
Writer가 데이터 수정
       ↓
Reader 기다림
```
읽기 요청이 많으면 성능이 크게 떨어질 수 있음

**2. MVCC**
```
Writer → 새로운 Version 수정

Reader → 이전 Version 조회
```
**Reader와 Writer 사이의 Lock 경쟁을 줄이고 동시성을 높일 수 있음**

## MVCC와 Isolation Level
MVCC는 Isolation Level을 구현하는 데 중요한 역할을 함

예를 들어 REPEATABLE READ에서는 하나의 Transaction이 처음 조회했던 시점에 맞는 Version을 계속 보여줄 수 있음
```
Transaction A 시작

balance = 100 조회
```
다른 Transaction이 수정
```
Transaction B

100 → 200
COMMIT
```
A가 다시 조회하더라도 MVCC를 통해
```
balance = 100
```

## Connection Pool
Spring Boot 백엔드와 Database를 연결해서 이해할 때 중요한 개념

요청이 들어올 때마다 DB Connection을 새로 생성하면 비용이 큼
```
Request
   ↓
DB Connection 생성
   ↓
SQL
   ↓
Connection 종료
```
Connection 생성에는
- TCP 연결
- 인증
- DB 세션 생성

등의 비용이 발생 가능

그래서 미리 Connection을 만들어 Pool에 보관
```
Spring Boot

Connection Pool
 ├─ Connection 1
 ├─ Connection 2
 ├─ Connection 3
 └─ Connection 4
        ↓
    Database
```
요청이 들어오면 Connection을 빌려 사용한 후 반환

Spring Boot에서는 **HikariCP**가 기본적으로 많이 사용됨

### Connection Pool이 모두 사용중이면?
Pool Size가 10일 때

10개의 요청이 모두 DB 작업을 수행하고 있다면 11번째 요청은 바로 DB를 사용할 수 없음

따라서 DB Query가 느려지면 Connection 반환도 늦어지고 장애가 확산될 수 있음
```
Slow Query
   ↓
Connection 오래 점유
   ↓
Pool 부족
   ↓
다른 요청 대기
   ↓
전체 API 지연
```

## Replication
**같은 데이터를 여러 Database Server에 복제하는 구조**
```
        Primary
          ↓
     Replication
       ↙      ↘
 Replica 1   Replica 2
```

대표적으로 **`Write -> Primary`**, **`Read -> Replica`** 구조를 사용할 수 있음

장점
- 읽기 부하 분산
- 장애 대응
- 가용성 향상

주의할 점
- Primary에서 변경된 데이터가 Replica에 즉시 반영되지 않을 수 있음
```
Primary UPDATE
       ↓
Replication Delay
       ↓
Replica
```

## Partitioning과 Sharding
데이터가 매우 많아지면 하나의 Database에 모든 데이터를 저장하기 어려울 수 있음

### Partitioning
하나의 Database/Table의 데이터를 논리적으로 나눔
```
orders

2024 데이터
2025 데이터
2026 데이터
```
날짜 등에 따라 Partition을 나눌 수 있음

### Sharding
데이터 자체를 여러 Database Server로 나눔
```
user_id 1~10000
      ↓
   Shard 1


user_id 10001~20000
      ↓
   Shard 2
```
장점
- 데이터와 트래픽 분산
- Scale Out 가능

단점
- Shard Key 선택
- JOIN
- Transaction
- 데이터 이동
- 운영 복잡도

등의 문제가 생기기 때문에 무조건 사용하는 것은 아님

## Spring의 @Transactional
```
@Transactional
public void transfer() {

    withdraw();
    deposit();

}
```
Spring은 일반적으로 **Proxy를 통해 Transaction을 처리**
```
Controller
    ↓
Transaction Proxy
    ↓
Transaction 시작
    ↓
Service Method
    ↓
SQL 실행
    ↓
성공 → Commit
실패 → Rollback
```

## 느린 API Database 확인 순서
```
API가 느림
   ↓
DB Query가 느린지 확인
   ↓
실제 SQL 확인
   ↓
실행 계획 확인
   ↓
Full Scan / Index Scan 확인
   ↓
조회 Row 수 확인
   ↓
Index 적절성 확인
   ↓
JOIN / 조건 개선
   ↓
Lock 대기 확인
   ↓
Connection Pool 확인
```
**측정 -> 실행 계획 -> 원인 확인 -> 개선**

## 우선 순위 질문

### Index는 왜 조회를 빠르게 하는가?
별도의 정렬된 탐색 구조를 만들어 전체 Table을 순차적으로 읽지 않고 필요한 데이터 위치를 빠르게 찾을 수 있기 때문임

### Index를 많이 만들면 무조건 좋은가?
Index도 저장 공간을 사용하며 INSERT, UPDAET, DELETE 시 Index도 함께 수정해야 하기 때문에 쓰기 비용이 증가함

### 복합 Index에서 Column 순서가 왜 중요한가?
Index가 지정된 Column 순서에 따라 정렬되기 때문임. 따라서 실제 조회 조건과 정렬 방식을 고려해 순서를 결정해야 함

### Query가 느리면 어디부터 확인하는가?
우선 실제 실행되는 SQL과 실행 계획을 확인하고 Full Scan 여부, Index 사용 여부, 조회 Row 수 등을 확인. 이후 JOIN이나 조건, Index를 개선하고 다시 측정

### Transcation이란?
여러 Database 작업을 하나의 논리적인 작업 단위로 묶어 모두 성공하거나 실패하도록 처리하는 것

### ACID란?
Transaction의 핵심 특성인 원자성, 일관성, 격리성, 영속성을 의미

### Isolation Level은 왜 필요한가?
여러 Transaction이 동시에 실행될 때 데이터 정합성과 동시 처리 성능 사이의 수준을 결정하기 위해 필요함

### Lock은 왜 필요한가?
여러 Transaction이 동시에 같은 데이터를 수정할 때 충돌과 데이터 정합성 문제를 막기 위해 사용

### Deadlock이란?
두 개 이상의 Transaction이 서로가 가진 Lock을 기다리면서 더 이상 진행하지 못하는 상태

### MVCC란?
데이터의 여러 Version을 관리하여 Reader와 Writer 사이의 Lock 경쟁을 줄이고 Transaction 격리와 동시성을 높이는 방식

### Connection Pool은 왜 사용하는가?
매 요청마다 Database Connection을 새로 만드는 비용을 줄이기 위해 Connection일 미리 만들어두고 재사용

## 핵심 흐름
```
Spring Boot 요청
       ↓
Connection Pool
       ↓
Database
       ↓
SQL 실행
       ↓
Index를 이용해 데이터 탐색
       ↓
Transaction으로 작업 단위 관리
       ↓
동시 요청 발생
       ↓
Isolation Level
 + Lock
 + MVCC
       ↓
데이터 정합성 유지
```
성능 문제 발생
```
느린 API
   ↓
SQL 확인
   ↓
실행계획 확인
   ↓
Index / 조회량 확인
   ↓
Lock 대기 확인
   ↓
Connection Pool 확인
   ↓
원인 개선 후 재측정
```
