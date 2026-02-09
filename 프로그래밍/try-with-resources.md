### try-with-resources (자원 자동 해제)

**정의**
- Java 7부터 도입된 기능으로, `try` 블록이 끝나면 **자동으로 자원(Resource)을 해제(close)** 해주는 문법
- 파일 입출력(I/O), 데이터베이스 연결(Connection), 소켓(Socket) 등 사용 후 반드시 닫아야 하는 자원들을 안전하고 간결하게 처리할 수 있음

**사용 조건**
- 해당 자원 클래스가 `java.lang.AutoCloseable` 인터페이스를 구현하고 있어야 함
- `try (...)` 괄호 안에 자원 생성 코드를 넣어야 함

---

### 기존 방식 (try-catch-finally)의 문제점

과거에는 `finally` 블록에서 직접 `close()`를 호출

```java
FileInputStream is = null;
BufferedInputStream bis = null;

try {
    is = new FileInputStream("file.txt");
    bis = new BufferedInputStream(is);
    // ... 작업 수행 ...
} catch (IOException e) {
    e.printStackTrace();
} finally {
    // 문제점 1: close()를 호출하다가 또 예외가 발생할 수 있음 (지저분한 중첩 try-catch)
    // 문제점 2: 실수로 close()를 빼먹으면 메모리 누수(Memory Leak) 발생
    if (bis != null) try { bis.close(); } catch (IOException e) { }
    if (is != null) try { is.close(); } catch (IOException e) { }
}
```

---

### 개선된 방식 (try-with-resources)

코드가 훨씬 간결해지고, 자원 해제가 보장됨

```java
// try 괄호 안에 자원을 선언 (세미콜론으로 여러 개 선언 가능)
try (
    FileInputStream is = new FileInputStream("file.txt");
    BufferedInputStream bis = new BufferedInputStream(is)
) {
    // ... 작업 수행 ...
    
} catch (IOException e) {
    e.printStackTrace();
}
// finally 블록이 없어도 자동으로 close()가 호출됨
// 자원은 선언된 순서의 역순(bis -> is)으로 닫힘
```

---

### 핵심 개념: Suppressed Exception (억제된 예외)

**상황**
- `try` 블록 안에서 예외 A가 발생 (주된 예외)
- 자원을 닫는 `close()` 메서드에서도 예외 B가 발생 (부차적인 예외)

**기존 방식의 문제**
- `finally`에서 발생한 예외 B가 예외 A를 덮어씌워 버림
- 개발자는 정작 중요한 예외 A(비즈니스 로직 에러)를 보지 못하고, 엉뚱한 예외 B(close 에러)만 보게 되어 디버깅이 힘들어짐

**try-with-resources의 해결책**
- 주된 예외 A를 던지고, 예외 B는 **"억제된 예외(Suppressed Exception)"** 로 등록하여 예외 A 안에 담아둠
- `e.getSuppressed()` 메서드로 나중에 예외 B를 꺼내볼 수 있음

**예시 코드**
```java
public class Resource implements AutoCloseable {
    public void doSomething() {
        throw new RuntimeException("메인 로직 에러 (중요!)");
    }

    @Override
    public void close() {
        throw new RuntimeException("자원 닫기 에러 (덜 중요)");
    }
}

// 실행 결과
try (Resource r = new Resource()) {
    r.doSomething();
} catch (RuntimeException e) {
    System.out.println("메인 예외: " + e.getMessage());
    
    // 억제된 예외 확인
    for (Throwable t : e.getSuppressed()) {
        System.out.println("억제된 예외: " + t.getMessage());
    }
}

/* 출력:
메인 예외: 메인 로직 에러 (중요!)
억제된 예외: 자원 닫기 에러 (덜 중요)
*/
```

---

### 요약: 왜 써야 하나요?

1.  **코드 간결성:** 지저분한 `finally` 블록과 `null` 체크가 사라짐
2.  **자원 누수 방지:** 개발자가 실수로 `close()`를 빼먹을 일이 없음
3.  **정확한 예외 처리:** 주된 예외가 덮어씌워지지 않고, 부가적인 예외까지 모두 확인할 수 있음 (Suppressed Exception)