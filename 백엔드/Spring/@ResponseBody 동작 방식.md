# @ResponseBody 및 ResponseEntity 동작 방식

Spring MVC에서 컨트롤러가 반환하는 데이터를 HTML 뷰(View)가 아닌 **HTTP 응답 본문(Body)에 직접 직렬화**하여 전송하는 메커니즘을 설명함

---

## 1. @ResponseBody 동작 원리

컨트롤러 메서드에 `@ResponseBody`가 붙으면 Spring은 다음과 같이 동작함

1.  **ViewResolver 대신 HttpMessageConverter 작동**: 반환값을 HTML 뷰 파일명으로 해석하지 않고, 응답 본문에 직접 쓰기 위해 적절한 메시지 컨버터를 선택함
2.  **직렬화(Serialization)**: 반환 객체의 타입과 클라이언트의 `Accept` 헤더를 확인하여, 객체를 JSON이나 XML 등으로 변환함 (주로 Jackson 라이브러리가 객체를 JSON으로 변환)
3.  **응답 전송**: 변환된 데이터를 HTTP Response Body에 담아 클라이언트에게 전송함

> **참고**: `@RestController`는 모든 메서드에 `@ResponseBody`가 자동으로 적용된 컨트롤러

---

## 2. @ResponseBody vs ResponseEntity

실무에서는 두 방식을 상황에 따라 혼용하거나 팀 컨벤션에 따라 선택함

### 1) @ResponseBody (단순 데이터 반환)
*   **특징**: 자바 객체만 반환하면 Spring이 자동으로 200 OK 상태 코드와 함께 데이터를 보냄
*   **장점**: 코드가 간결하고 직관적
*   **단점**: HTTP 상태 코드나 헤더를 상황에 따라 동적으로 변경하기 어려움 (별도의 `@ResponseStatus` 사용 필요)

### 2) ResponseEntity<T> (데이터 + 메타데이터 반환)
*   **특징**: HTTP 응답의 세 요소(**상태 코드, 헤더, 본문**)를 개발자가 직접 제어할 수 있는 Wrapper 객체
*   **장점**: 상황에 따라 다른 상태 코드(201, 400, 404 등)를 유연하게 반환할 수 있음
*   **단점**: `@ResponseBody`에 비해 코드가 다소 길어질 수 있음

---

## 3. 실무 예시

### 시나리오 1: 단순 조회 성공 (200 OK)
조회 기능은 대부분 성공하므로 코드가 간결한 `@ResponseBody` (또는 `@RestController`) 방식을 주로 사용합니다.

```java
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    // 성공 시 자동으로 200 OK 응답
    return userService.findById(id);
}
```

### 시나리오 2: 리소스 생성 성공 (201 Created)
새로운 데이터를 생성했을 때는 표준에 따라 201 상태 코드와 생성된 리소스의 경로(Location)를 헤더에 담아주는 것이 좋음. 이럴 때 `ResponseEntity`가 필수적

```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
    UserResponse response = userService.create(request);
    
    return ResponseEntity
            .status(HttpStatus.CREATED) // 201 상태 코드
            .header("Location", "/users/" + response.getId()) // 헤더 추가
            .body(response); // 응답 바디
}
```

### 시나리오 3: 조건에 따른 에러 반환
비즈니스 로직에 따라 서로 다른 상태 코드를 반환해야 할 때 사용함

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    if (authService.isValid(request)) {
        return ResponseEntity.ok(new LoginResponse("success"));
    }
    // 실패 시 401 Unauthorized 반환
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
}
```

---

## 4. 요약 및 권장 사항

| 구분 | @ResponseBody | ResponseEntity<T> |
| :--- | :--- | :--- |
| **반환 타입** | 일반 자바 객체 (DTO, List 등) | Spring 제공 Wrapper 객체 |
| **상태 코드 제어** | 고정적 (기본 200) | **동적/유연함** |
| **헤더 제어** | 어려움 | **쉬움** |
| **권장 상황** | 단순 데이터 조회, 전역 예외 처리가 구축된 환경 | 세밀한 응답 제어가 필요한 경우, 생성/수정/삭제 API |

### 결론
> **실무에서는 전역 예외 처리기(@RestControllerAdvice)를 잘 구축해두고, 성공 시에는 `@ResponseBody`로 간결하게 반환하며, 특별한 상태 코드나 헤더 조작이 필요한 경우에만 `ResponseEntity`를 사용하는 방식이 인기가 높음**
