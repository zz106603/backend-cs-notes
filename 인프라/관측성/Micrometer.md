### Micrometer (마이크로미터)

**정의**
- **"애플리케이션 메트릭(Metrics)을 위한 SLF4J"** 라고 불림
- 로깅을 할 때 Logback, Log4j2 등 구현체를 신경 쓰지 않고 SLF4J 인터페이스만 쓰듯이, **모니터링 시스템(Prometheus, Datadog 등)이 무엇이든 상관없이 표준화된 방법으로 메트릭을 수집**할 수 있게 해주는 라이브러리
- Spring Boot 2.0부터 기본 메트릭 수집기로 내장되어 있음

---

### 전체 동작 흐름 (Architecture)

**[Spring Boot App]** -> **[Actuator]** -> **[Micrometer]** -> **[모니터링 시스템]**

1.  **Spring Boot Actuator:** 애플리케이션의 상태 정보를 수집 (CPU, 메모리, HTTP 요청 수 등)
2.  **Micrometer:** 수집된 정보를 특정 모니터링 시스템이 이해할 수 있는 포맷으로 변환 (추상화 계층)
3.  **모니터링 시스템 (예: Prometheus):** 주기적으로 애플리케이션에 접속하여 메트릭 데이터를 긁어감 (Pull 방식)
4.  **시각화 도구 (예: Grafana):** 수집된 데이터를 그래프로 보여줌

---

### 실무 활용: Spring Boot Actuator와 연동

**1. 의존성 추가 (build.gradle)**
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    // Prometheus와 연동하려면 아래 의존성 추가 (Micrometer 구현체)
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

**2. 설정 (application.yml)**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, info # prometheus 엔드포인트 노출
  metrics:
    tags:
      application: my-service # 모든 메트릭에 공통 태그 추가 (서버 구분용)
```

**3. 결과 확인**
- 브라우저에서 `http://localhost:8080/actuator/prometheus`로 접속하면, Prometheus 포맷으로 변환된 텍스트 데이터를 볼 수 있음

---

### 실무 코드 예시: 커스텀 메트릭 만들기

기본적인 CPU, 메모리 외에 **"우리 서비스만의 비즈니스 지표"** 를 측정하고 싶을 때 사용

**상황:** 쇼핑몰에서 **"실시간 주문 건수"** 와 **"결제 처리 시간"** 을 모니터링하고 싶음

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Counter orderCounter; // 주문 건수 (계속 증가)
    private final Timer paymentTimer;   // 결제 시간 (시간 측정)

    // MeterRegistry는 스프링이 자동으로 주입해줍니다.
    public OrderService(MeterRegistry registry) {
        // 1. Counter 등록: 누적 주문 수
        this.orderCounter = Counter.builder("my.order.count")
                .tag("type", "online") // 태그로 데이터 분류 가능
                .description("Total number of orders")
                .register(registry);

        // 2. Timer 등록: 결제 로직 수행 시간
        this.paymentTimer = Timer.builder("my.payment.time")
                .description("Time taken for payment processing")
                .register(registry);
    }

    public void order(String item) {
        // 주문 로직 수행...
        
        // 주문 건수 1 증가
        orderCounter.increment(); 
        
        // 결제 시간 측정 (람다식으로 감싸서 실행 시간 자동 기록)
        paymentTimer.record(() -> {
            processPayment(); // 실제 결제 로직 (예: 0.5초 소요)
        });
    }

    private void processPayment() {
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }
}
```

**주요 메트릭 타입**
1.  **Counter:** 단조 증가하는 값 (예: 총 요청 수, 에러 발생 횟수)
2.  **Gauge:** 오르락내리락하는 값 (예: 현재 CPU 사용량, 현재 대기 중인 큐의 크기)
3.  **Timer:** 시간을 측정 (예: API 응답 속도, DB 쿼리 실행 시간)

---

### 외부 모니터링 시스템 적용 (Prometheus & Grafana)

**1. Prometheus (데이터 수집)**
- `prometheus.yml` 설정 파일에 우리 서버의 Actuator 주소를 등록
- Prometheus가 15초마다(설정 가능) `/actuator/prometheus`를 호출하여 데이터를 가져감 (Scraping)

**2. Grafana (데이터 시각화)**
- 데이터 소스로 Prometheus를 연결
- 대시보드 패널을 만들고 쿼리를 입력
    - 예: `rate(my_order_count_total[1m])` -> 최근 1분간 주문 증가율 그래프
- 알람(Alert)을 설정하여 에러율이 치솟으면 슬랙(Slack)으로 알림을 보냄