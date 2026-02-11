### Thread-Safe (스레드 안전)

**정의**
- 멀티 스레드 환경에서 여러 스레드가 동시에 하나의 객체나 변수(공유 자원)에 접근해도, **프로그램의 실행 결과가 올바르게 유지되는 성질**을 말함
- 즉, 어떤 스레드가 언제 실행되든 간에 데이터가 깨지거나 엉뚱한 값이 나오지 않아야 함

---

### Thread-Safe를 달성하기 위한 조건 (방법)

**1. 재진입성 (Re-entrancy)**
- 어떤 함수가 한 스레드에서 실행 중일 때, 다른 스레드가 그 함수를 호출하더라도 결과에 문제가 없어야 함
- 주로 **전역 변수(Global Variable)나 정적 변수(Static Variable)를 사용하지 않고**, 지역 변수만 사용하면 달성할 수 있음

**2. 상호 배제 (Mutual Exclusion)**
- 공유 자원(변수, 파일 등)에 접근할 때, **한 번에 하나의 스레드만 접근**하도록 막는 것
- **Java 예시:** `synchronized` 키워드, `ReentrantLock` 사용
- **단점:** 성능 저하가 발생할 수 있음 (병목 현상)

**3. 스레드 로컬 저장소 (Thread-Local Storage)**
- 각 스레드마다 **자신만의 전용 저장소**를 만들어 공유 자원 사용을 피하는 것
- **Java 예시:** `ThreadLocal` 클래스 사용 (예: 웹 요청마다 별도의 DB 커넥션 할당)

**4. 불변 객체 (Immutable Object)**
- 객체 생성 후 내부 상태가 절대 변하지 않는 객체를 사용
- 읽기 전용(Read-Only)이므로 여러 스레드가 동시에 접근해도 안전
- **Java 예시:** `String`, `Integer`, `Collections.unmodifiableList()`

**5. 원자 연산 (Atomic Operation)**
- 더 이상 쪼갤 수 없는 연산을 사용하여, 중간에 다른 스레드가 끼어들지 못하게 함
- **Java 예시:** `AtomicInteger`, `AtomicLong` (CAS 알고리즘 사용)

---

### Java에서의 Thread-Safe 구현 예시

**1. synchronized (동기화 메서드/블록)**
```java
public class Counter {
    private int count = 0;
    
    // 한 번에 하나의 스레드만 실행 가능 (상호 배제)
    public synchronized void increment() {
        count++;
    }
}
```

**2. Concurrent 패키지 (java.util.concurrent)**
- `HashMap` 대신 `ConcurrentHashMap` 사용
- `ArrayList` 대신 `CopyOnWriteArrayList` 사용
- 내부적으로 락(Lock)을 세분화하거나 CAS 알고리즘을 사용하여 `synchronized`보다 성능이 좋음

**3. 불변 객체 사용**
```java
// 상태를 변경할 수 없으므로 안전함
public final class ImmutablePoint {
    private final int x;
    private final int y;
    
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
```

**4. ThreadLocal 사용**
```java
public class UserContext {
    // 각 스레드마다 별도의 저장 공간을 가짐
    private static final ThreadLocal<String> userHolder = new ThreadLocal<>();
    
    public static void setUser(String user) {
        userHolder.set(user);
    }
    
    public static String getUser() {
        return userHolder.get();
    }
}
```

---

### 실무 팁: 서블릿(Servlet)과 스프링 빈(Spring Bean)

- **서블릿과 스프링 빈은 기본적으로 싱글톤(Singleton)**
- 즉, 하나의 객체를 여러 스레드(사용자 요청)가 공유함
- 따라서 **인스턴스 변수(필드)에 상태를 저장하면 Thread-Safe하지 않음**
- **해결책:** 상태 정보는 지역 변수(메서드 내부)나 파라미터로 주고받아야 하며, 필드에는 읽기 전용 상수나 불변 객체만 저장해야 함