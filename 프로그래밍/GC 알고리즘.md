# Java GC 알고리즘 (Garbage Collection)

실무에서는 **"지금 우리가 쓰는 버전의 Default GC가 무엇이고, 어떤 특징이 있는가?"** 를 아는 것이 가장 중요함

---

## 1. Java 버전별 Default GC (핵심)

| Java 버전 | Default GC | 특징 |
| :--- | :--- | :--- |
| **Java 8** | **Parallel GC** | 처리량(Throughput) 중심. Stop-The-World(STW) 시간이 긺 |
| **Java 11** | **G1 GC** | 응답 시간(Latency)과 처리량의 균형. 대용량 힙에 적합 |
| **Java 17** | **G1 GC** | G1 GC 성능이 대폭 개선됨. (Parallel GC보다 훨씬 빠름) |
| **Java 21** | **G1 GC** | 여전히 Default는 G1이지만, **Generational ZGC**가 정식 도입됨 |

> **실무 팁**: Java 8에서 Java 17로 버전만 올려도 GC 성능 개선으로 인해 애플리케이션 처리량이 약 10~20% 향상될 수 있음

---

## 2. 현재의 표준: G1 GC (Garbage First)

Java 9부터 기본으로 채택되어 현재까지 가장 널리 쓰이는 GC

### 핵심 동작 원리: "Region"
*   과거의 GC(Serial, Parallel)는 힙 메모리를 물리적으로 Young/Old 영역으로 딱 잘라 나누었음
*   **G1 GC**는 힙을 바둑판처럼 잘게 쪼갠 **Region(지역)** 단위로 관리
*   각 Region은 동적으로 Young이 되기도 하고 Old가 되기도 함

### 왜 "Garbage First" 인가요?
*   이름 그대로 **"쓰레기가 많은 곳(Garbage First)을 먼저 청소한다"** 는 뜻
*   전체 힙을 다 뒤지는 것이 아니라, 살아있는 객체가 적은(수거할 쓰레기가 많은) Region만 골라서 청소하므로 효율적이고 STW 시간을 줄일 수 있음

### 장점
*   **예측 가능한 STW**: "GC 멈춤 시간을 200ms 안으로 맞춰줘"라고 설정하면 최대한 그 목표를 맞추려고 노력함
*   **대용량 힙 최적화**: 수십 GB 이상의 힙 메모리에서도 안정적으로 동작함

---

## 3. 차세대 GC: ZGC (Z Garbage Collector)

Java 15에서 정식 출시되었고, **Java 21에서 "Generational ZGC"로 진화**하며 완성형이 됨

### 목표: "STW 1ms 미만"
*   G1 GC도 훌륭하지만, 수백 GB ~ 수 TB 단위의 초대용량 힙에서는 STW 시간이 길어질 수 있음
*   ZGC는 힙 크기가 아무리 커져도 **Stop-The-World 시간이 1ms 미만**으로 유지되는 것을 목표로 함 (거의 멈추지 않음)

### Java 21의 혁신: Generational ZGC
*   기존 ZGC는 Young/Old 세대 구분이 없어서(Non-generational), 짧게 살다 죽는 객체 처리에 비효율적이었음
*   **Java 21**부터 ZGC도 G1처럼 **세대(Generation)를 구분**하게 됨
*   결과적으로 **G1 GC보다 처리량은 비슷하거나 더 좋으면서, 멈춤 시간은 비교도 안 되게 짧은** 괴물 같은 성능을 보여줌

### 언제 사용하나요?
*   **초저지연(Low Latency)이 필수인 서비스**: 실시간 주식 거래, 게임 서버 등
*   **초대용량 메모리 사용**: 수백 GB 이상의 힙을 사용하는 빅데이터 분석 시스템
*   사용법: `-XX:+UseZGC -XX:+ZGenerational` (Java 21 기준)

---

## 4. 요약: 무엇을 써야 할까요?

1.  **일반적인 웹 애플리케이션 (Spring Boot 등)**
    *   그냥 **기본값(G1 GC)** 을 쓰자. Java 17/21의 G1 GC는 이미 충분히 훌륭함
    *   튜닝보다는 힙 메모리 크기(`-Xms`, `-Xmx`)를 적절히 설정하는 것이 더 중요함

2.  **응답 속도가 매우 중요한 특수 목적 시스템**
    *   **Java 21 이상**을 쓴다면 **ZGC** 도입을 적극 검토
    *   GC 튜닝으로 고통받는 것보다 ZGC를 켜는 것이 훨씬 효과적일 수 있음

3.  **메모리가 매우 작은 환경 (1GB 이하 컨테이너)**
    *   **Serial GC**가 더 나을 수 있음. 오버헤드가 가장 적기 때문
    *   `-XX:+UseSerialGC`
