# ThreadLocal (스레드 로컬)

각 스레드(Thread)마다 **독립적인 저장 공간**을 제공하여, 같은 변수에 접근하더라도 스레드별로 다른 값을 가질 수 있도록 하는 기술

---

## 1. 왜 필요한가요?

### 문제 상황: "모든 메서드에 사용자 정보 넘기기"
웹 애플리케이션에서는 요청이 들어올 때마다 스레드 풀의 스레드가 하나씩 할당됨. 이때 요청한 사용자의 정보(ID, 권한 등)를 로깅이나 비즈니스 로직 처리를 위해 서비스 계층 깊은 곳까지 전달해야 함

#### Bad: 모든 메서드에 파라미터 추가 (Parameter Drilling)
```java
// Controller
public void updateUser(AuthInfo authInfo, UserData data) {
    userService.update(authInfo, data);
}

// UserService
public void update(AuthInfo authInfo, UserData data) {
    // ... 비즈니스 로직 ...
    logService.saveLog(authInfo, "사용자 정보 수정");
}

// LogService
public void saveLog(AuthInfo authInfo, String message) {
    // authInfo를 사용하여 로그 저장
    System.out.println(authInfo.getUserId() + ": " + message);
}
```
*   **문제점**: 단지 `LogService`에서만 필요한 `authInfo` 때문에 관련 없는 모든 메서드의 시그니처가 오염됨. 유지보수가 매우 불편해짐

---

## 2. 해결 방법: ThreadLocal 사용

`ThreadLocal`을 사용하면 요청의 시작 지점(Filter, Interceptor)에서 사용자 정보를 저장하고, 애플리케이션의 어느 곳에서든 파라미터 없이 바로 꺼내 쓸 수 있음

### Good: ThreadLocal로 컨텍스트 전파
**1. ThreadLocal을 관리하는 컨텍스트 홀더 생성**
```java
public class UserContextHolder {
    private static final ThreadLocal<AuthInfo> userContext = new ThreadLocal<>();

    public static void set(AuthInfo authInfo) {
        userContext.set(authInfo);
    }

    public static AuthInfo get() {
        return userContext.get();
    }

    public static void remove() {
        userContext.remove();
    }
}
```

**2. 요청 진입점에서 데이터 저장, 이탈점에서 데이터 제거 (Interceptor/Filter)**
```java
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // 1. 요청 헤더에서 사용자 정보를 파싱
        AuthInfo authInfo = ...; 
        // 2. ThreadLocal에 저장
        UserContextHolder.set(authInfo);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, ...) {
        // 3. 요청 처리가 끝나면 반드시 제거!
        UserContextHolder.remove();
    }
}
```

**3. 이제 어디서든 파라미터 없이 사용 가능**
```java
// LogService (매우 깔끔해짐)
public void saveLog(String message) {
    AuthInfo authInfo = UserContextHolder.get(); // 파라미터 없이 바로 꺼내 씀
    System.out.println(authInfo.getUserId() + ": " + message);
}
```

---

## 3. 가장 중요한 주의사항: 메모리 누수 (Memory Leak)

### 원인: 스레드 풀 (Thread Pool) 환경
*   Tomcat 같은 웹 서버는 스레드를 재사용함. 요청 처리가 끝나도 스레드는 죽지 않고, 다른 요청을 처리하기 위해 대기함
*   만약 `UserContextHolder.remove()`를 호출하지 않으면, 해당 스레드의 `ThreadLocalMap`에는 이전에 사용했던 `AuthInfo` 객체가 **계속 남아있게 됨**

### 시나리오
1.  **요청 A (사용자 "Kim")** 가 **스레드-1**에 할당됨. `ThreadLocal`에 "Kim"의 정보가 저장됨
2.  요청 A 처리가 끝나고 응답이 나감 (이때 `remove()`를 안 했다고 가정)
3.  잠시 후, **요청 B (사용자 "Lee")** 가 우연히 같은 **스레드-1**에 할당됨
4.  요청 B의 로직 어딘가에서 `UserContextHolder.get()`을 호출하면, 의도치 않게 이전에 남아있던 **"Kim"의 정보가 반환**됨. 이는 심각한 보안 사고로 이어질 수 있음
5.  이런 스레드가 계속 쌓이면 아무도 참조하지 않는 쓰레기 객체들이 메모리를 계속 차지하여 결국 **`OutOfMemoryError`** 가 발생함

### 해결책
> **요청 처리가 끝나면 `finally` 블록이나 `Interceptor.afterCompletion`에서 반드시 `remove()`를 호출해야 함**

---

## 4. 대표적인 실무 사용처
*   **Spring Security**: `SecurityContextHolder`가 `ThreadLocal`을 사용하여 현재 인증된 사용자의 정보(`Authentication` 객체)를 관리함
*   **JPA 트랜잭션 관리**: `EntityManager`나 DB 세션을 `ThreadLocal`에 바인딩하여, 같은 스레드 내에서는 동일한 트랜잭션 컨텍스트를 유지하도록 함
