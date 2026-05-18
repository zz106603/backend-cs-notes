# Connection Pool

DB Connection을 미리 생성해두고 재사용하는 기술

매 요청마다 Connection을 새로 생성하지 않고:

```
요청
→ Pool에서 Connection 대여
→ SQL 실행
→ Pool 반납
```

방식으로 동작

---

## 1. Connection Pool의 필요성

### 1) Connection 생성 비용 감소

DB Connection 생성 시:

- TCP 연결
- 인증
- 세션 생성

과정 필요

매 요청마다 반복 시:

- 응답 속도 저하
- DB CPU 사용량 증가
- 네트워크 비용 증가

문제 발생 가능

Connection 재사용으로 비용 감소 가능

---

### 2) DB 리소스 보호

DB는 동시에 처리 가능한 Connection 수 제한 존재

예:

```
DB max_connections = 100
```

Connection을 무제한으로 생성하면:

- DB 과부하
- Connection 거절
- 전체 장애

발생 가능

Connection Pool은 Connection 개수를 제한하여 DB를 보호함

---

### 3) 애플리케이션 안정성 확보

동시 요청 증가 시:

```
Connection 생성 폭증
→ DB 부하 증가
→ 응답 지연
→ Timeout 증가
```

Pool 사용 시:

- Connection 수 제한 가능
- 요청 처리량 제어 가능
- 안정적인 운영 가능

---

## 2. 동작 방식

### 기본 흐름

```
Application Start
→ Connection 미리 생성
→ Pool 저장

Request
→ Connection Borrow
→ Query 실행
→ Connection Return
```

핵심:

- Connection 재사용
- Borrow / Return 구조
- Connection 생성 비용 최소화

---

## 3. 주요 설정 (HikariCP 기준)

| 설정 | 의미 |
|---|---|
| maximumPoolSize | 최대 Connection 수 |
| minimumIdle | 최소 Idle Connection 유지 |
| connectionTimeout | Connection 획득 대기 시간 |
| maxLifetime | Connection 최대 수명 |
| idleTimeout | Idle Connection 유지 시간 |
| leakDetectionThreshold | Connection Leak 감지 시간 |

---

## 4. Pool Size 문제

### Pool 부족

```text
maximumPoolSize 부족
→ Connection 대기 증가
→ Timeout 발생
```

대표 오류:

```text
Connection is not available
```

---

### Pool 과다

```text
Pool 과도 증가
→ DB Connection 폭증
→ DB CPU 증가
→ 성능 저하
```

Pool 크기는:

- DB 스펙
- 트래픽
- 서버 수

기준으로 조절 필요

---

## 5. Connection Leak

Connection 반납 누락 상황

```java
Connection conn = pool.getConnection();
```

close() 누락 시:

```text
Pool Connection 고갈
→ 신규 요청 실패
```

대표 오류:

```text
Connection is not available
```

실무에서는:

```yaml
leak-detection-threshold
```

설정으로 Leak 추적 가능

---

## 6. DB / Pool Timeout 불일치 문제

문제 상황:

```text
DB wait_timeout < Hikari maxLifetime
```

결과:

- DB는 Connection 종료
- Pool은 살아있다고 판단
- 죽은 Connection 사용 오류 발생 가능

실무 일반 설정:

```text
Hikari maxLifetime
< DB wait_timeout
```

---

## 7. MSA 환경 문제

MSA에서는 서비스마다 Connection Pool 존재

```text
User Service
Order Service
Payment Service
Notification Service
```

서비스 수 증가 시:

- 전체 Connection 수 증가
- RDS max_connections 초과 가능

실무에서 자주 발생하는 장애 포인트

---

## 8. Spring Boot와 HikariCP

Spring Boot 기본 Connection Pool은 HikariCP 사용

특징:

- 빠른 성능
- 낮은 오버헤드
- 높은 안정성

설정 예시:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      max-lifetime: 600000
```

---

## 9. 핵심 포인트

### Connection Pool 목적

- Connection 생성 비용 감소
- Connection 재사용
- 응답 속도 개선
- DB 부하 감소
- 시스템 안정성 확보

---

### 운영 핵심 포인트

- DB max_connections 고려 필요
- 서버 수 증가 시 전체 Connection 증가
- Pool 과다 설정 주의
- Connection Leak 모니터링 필요
- DB timeout / Pool timeout 정합성 중요

---

## 10. 면접 포인트

### Connection Pool을 사용하는 이유

- DB Connection 생성 비용 감소
- Connection 재사용
- 성능 및 안정성 확보 목적

---

### HikariCP를 사용하는 이유

- 빠름
- 가벼움
- 안정성 높음
- Spring Boot 기본 지원

---

### Pool 크기를 크게 하면 좋은가?

아님

문제:

- DB max_connections 초과 가능
- DB 부하 증가 가능

고려 요소:

- 트래픽
- DB 스펙
- 서버 수
- 쿼리 처리 시간
