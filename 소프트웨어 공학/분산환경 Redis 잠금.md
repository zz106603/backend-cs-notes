### 분산 환경에서 Redis를 활용한 잠금 (Distributed Lock with Redis)

분산 환경에서는 여러 서버가 동시에 공유 자원에 접근할 때 데이터 정합성 문제가 발생할 수 있음. 
이를 해결하기 위해 Redis를 활용하여 **분산 락(Distributed Lock)** 을 구현할 수 있습니다.

---

### 1. 기본 구현 방식 (Simple Implementation)

가장 간단한 방법은 Redis의 `SET` 명령어와 `NX` (Not Exists) 옵션을 사용하는 것

**동작 원리**
1.  **잠금 획득 (Lock Acquisition):**
    - 클라이언트는 `SET key value NX PX milliseconds` 명령어를 전송
    - `NX`: 키가 존재하지 않을 때만 값을 설정 (락 획득 성공)
    - `PX`: 락의 만료 시간(TTL)을 설정 (서버 장애 시 데드락 방지)
    - *예시:* `SET lock:product:123 random_value NX PX 10000`
2.  **작업 수행:** 락을 획득한 서버만 임계 영역(Critical Section)에 진입하여 작업을 수행
3.  **잠금 해제 (Lock Release):**
    - 작업이 끝나면 `DEL` 명령어로 키를 삭제하여 락을 해제
    - **주의:** 자신이 만든 락만 지워야 하므로, 키에 저장된 `random_value`가 자신이 설정한 값과 일치하는지 확인 후 삭제해야 함 (Lua 스크립트 사용 권장)

**한계점 (문제점)**
- **단일 실패 지점 (SPOF):** Redis 서버가 한 대라면, 해당 서버가 죽었을 때 전체 락 기능이 마비됨
- **Failover 시 락 유실:** Redis를 Master-Slave(Replica) 구조로 운영할 때, Master에서 락을 획득한 직후 복제(Replication)가 되기 전에 Master가 죽으면, Slave가 승격되면서 락 정보가 유실될 수 있음. 이로 인해 두 클라이언트가 동시에 락을 획득하는 상황이 발생할 수 있음

---

### 2. RedLock 알고리즘 (RedLock Algorithm)

단일 Redis 노드 구성의 한계(Failover 시 락 유실)를 극복하기 위해 Redis 창시자가 제안한 알고리즘

**전제 조건**
- 서로 독립적인 N개의 Redis 마스터 노드를 운영 (보통 5개 권장)
- 이 노드들은 서로 복제(Replication)를 하지 않음

**동작 과정 (예: 5개의 노드)**
1.  **시간 측정 시작:** 현재 시간을 기록
2.  **순차적 락 요청:** 5개의 노드에 순차적으로 락 획득을 시도합니다.
    - 이때 각 요청에 대한 타임아웃을 짧게 설정하여, 응답이 없는 노드는 빠르게 건너뜀
3.  **과반수 확인:** **N/2 + 1개 (여기서는 3개)** 이상의 노드에서 락을 획득했는지 확인
4.  **유효 시간 검증:** (현재 시간 - 시작 시간)이 락의 유효 시간(TTL)보다 작은지 확인
    - 락 획득에 걸린 시간이 너무 길면 락이 유효하지 않다고 판단
5.  **성공/실패 처리:**
    - **성공:** 남은 유효 시간 동안 작업을 수행
    - **실패:** 락을 획득했던 모든 노드에 해제 요청을 보냄

**장점**
- 일부 Redis 노드가 장애를 일으켜도 과반수 이상이 살아있다면 락 서비스가 유지됨
- 복제 지연으로 인한 락 유실 문제를 구조적으로 해결

---

### 3. 실무에서의 활용 (Java/Spring 예시)

직접 명령어를 구현하기보다는 검증된 라이브러리를 사용하는 것이 안전

**Redisson 라이브러리**
- Java 진영에서 가장 널리 쓰이는 Redis 클라이언트
- **특징:**
    - **Spin Lock 방식이 아님:** Pub/Sub 방식을 사용하여 락이 해제될 때 알림을 받아 재시도하므로 Redis 부하가 적음 (Lettuce는 Spin Lock 방식이라 부하가 클 수 있음)
    - **RLock 인터페이스:** `java.util.concurrent.locks.Lock` 인터페이스를 구현하여 사용이 익숙함
    - **WatchDog:** 락을 잡고 있는 동안 작업이 길어지면 자동으로 락 만료 시간을 연장해주는 기능을 제공

**코드 예시 (Pseudo Code)**
```java
RLock lock = redissonClient.getLock("lock:product:123");

try {
    // 10초 동안 락 획득 대기, 락 획득 후 2초간 점유
    boolean isLocked = lock.tryLock(10, 2, TimeUnit.SECONDS);

    if (isLocked) {
        // 임계 영역: 재고 감소 로직 등 수행
        processOrder();
    } else {
        // 락 획득 실패 처리
        throw new RuntimeException("잠시 후 다시 시도해주세요.");
    }
} catch (InterruptedException e) {
    // 에러 처리
} finally {
    // 락 해제 (현재 스레드가 락을 보유 중일 때만)
    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

---

추가 학습 자료: https://helloworld.kurly.com/blog/distributed-redisson-lock/
