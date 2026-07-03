# 헬스 체크 (Health Check)

서버나 애플리케이션이 **"정상적으로 요청을 처리할 수 있는 상태인지"** 주기적으로 확인하는 과정

로드 밸런서(Load Balancer)나 오케스트레이션 도구(Kubernetes)가 이 결과를 바탕으로 트래픽을 보낼지 말지 결정함

---

## 1. 왜 필요한가요? (실무 관점)

### 상황: 쇼핑몰 서버 중 1대가 고장남
*   서버 3대(A, B, C)가 로드 밸런서 뒤에서 동작 중
*   갑자기 **서버 B**의 메모리가 가득 차서(OOM) 응답을 못 하는 상태가 됨
*   **헬스 체크가 없다면?**: 로드 밸런서는 서버 B가 죽은 줄 모르고 계속 요청을 보냄. 사용자 3명 중 1명은 에러 페이지를 보게 됨
*   **헬스 체크가 있다면?**: 로드 밸런서는 서버 B가 "비정상(Unhealthy)"임을 감지하고, **트래픽을 즉시 차단**함. 모든 요청은 살아있는 A, C 서버로만 전달되어 사용자는 에러를 겪지 않음

---

## 2. 헬스 체크의 종류 (Liveness vs Readiness)

단순히 "살아있다"는 것만으로는 부족함. 쿠버네티스(Kubernetes) 환경에서는 목적에 따라 두 가지로 구분하여 사용함

### 1) Liveness Probe (생존 확인)
*   **목적**: "애플리케이션 프로세스가 살아있는가?"
*   **동작**: 실패하면 컨테이너를 **재시작(Restart)** 시킴
*   **실무 예시**:
    *   서버가 데드락(Deadlock)에 걸려 멈춰있거나, 메모리 누수로 인해 아무런 응답을 못 하는 경우
    *   이때는 재부팅만이 답이므로 Liveness Probe가 이를 감지하여 재시작

### 2) Readiness Probe (준비 확인)
*   **목적**: "지금 당장 트래픽을 받을 준비가 되었는가?"
*   **동작**: 실패하면 로드 밸런서에서 **제외(Traffic Cut)** 시킴 (재시작하지 않음)
*   **실무 예시**:
    *   **초기 구동 중**: Spring Boot 애플리케이션이 켜지고 있지만, 아직 DB 연결이나 캐시 로딩이 안 끝난 상태. 프로세스는 살아있지만 요청을 받으면 에러가 남
    *   **일시적 과부하**: 트래픽이 몰려 잠시 응답이 지연되는 경우. 이때 재시작하면 오히려 상황이 악화되므로, 잠시 트래픽만 끊어주어 회복할 시간을 줌

---

## 3. 구현 방법 및 Best Practice

### 1) 전용 API 엔드포인트 만들기
가장 일반적인 방법은 HTTP 요청을 받아 200 OK를 반환하는 API를 만드는 것

```java
@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        // 1. 단순히 200 OK만 반환 (Liveness 용)
        return ResponseEntity.ok("OK");
    }
    
    @GetMapping("/ready")
    public ResponseEntity<String> readinessCheck() {
        // 2. DB 연결, Redis 연결 등 필수 의존성 확인 (Readiness 용)
        if (!isDatabaseConnected() || !isRedisConnected()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build(); // 503
        }
        return ResponseEntity.ok("READY");
    }
}
```

### 2) Spring Boot Actuator 활용 (권장)
Spring Boot를 사용한다면 직접 구현할 필요 없이 `Actuator` 라이브러리를 사용하는 것이 표준

*   **의존성 추가**: `implementation 'org.springframework.boot:spring-boot-starter-actuator'`
*   **설정 (`application.yml`)**:
    ```yaml
    management:
      endpoint:
        health:
          probes:
            enabled: true # Liveness, Readiness 프로브 자동 활성화
    ```
*   **결과**:
    *   `/actuator/health/liveness`: 애플리케이션 상태 확인
    *   `/actuator/health/readiness`: DB, Disk Space 등 연동 상태까지 확인

### 3) 주의사항: Deep Health Check의 위험성
*   헬스 체크 API에서 **너무 많은 것을 검사하면 안 됨**
*   예를 들어, 헬스 체크 로직에 "복잡한 DB 쿼리"나 "외부 결제 API 호출"을 포함시켰다고 가정
*   외부 결제 서비스가 점검 중이라 응답을 안 주면, 우리 서버는 멀쩡한데도 헬스 체크가 실패하여 **모든 서버가 재시작되거나 트래픽이 차단되는 대형 사고(Cascading Failure)** 가 발생할 수 있음
*   **원칙**: 헬스 체크는 **"우리 서버의 상태"** 에 집중해야 하며, 외부 의존성은 신중하게 포함해야 함
