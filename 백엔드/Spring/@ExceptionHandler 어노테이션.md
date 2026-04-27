# @ExceptionHandler 어노테이션

Spring MVC 환경에서 발생하는 예외를 가로채어 사용자 정의 응답을 반환할 수 있도록 해주는 어노테이션

---

## 1. 주요 특징 및 동작 방식

### 핵심 특징
*   **세밀한 예외 처리**: 특정 컨트롤러 내부에서만 동작하거나, 전역적으로 모든 예외를 통합 관리할 수 있음
*   **HTTP 상태 코드 제어**: 예외 발생 시 단순히 500 에러가 아닌, 상황에 맞는 적절한 상태 코드(400, 404 등)를 반환할 수 있음
*   **유연한 응답 포맷**: JSON, HTML 등 클라이언트가 원하는 형식으로 에러 정보를 제공할 수 있음

### 동작 원리
1.  컨트롤러 로직 수행 중 예외(Exception)가 발생함
2.  `DispatcherServlet`이 예외를 감지하고 `ExceptionHandlerExceptionResolver`에게 처리를 위임함
3.  리졸버는 해당 예외를 처리할 수 있는 `@ExceptionHandler`가 붙은 메서드를 찾음
4.  찾았다면 해당 메서드를 실행하고 결과를 반환하며, 예외는 거기서 처리되어 WAS까지 전달되지 않음

---

## 2. 실무 예시: 전역 예외 처리 (Global Exception Handling)

실무에서는 개별 컨트롤러마다 예외 처리를 중복해서 작성하지 않고, **`@RestControllerAdvice`와 결합하여 전역적으로 관리**하는 것이 표준

### 1) 커스텀 에러 응답 객체 (ErrorResponse)
일관된 에러 형식을 클라이언트에게 제공하기 위해 정의함
```java
public record ErrorResponse(String code, String message) {}
```

### 2) 전역 예외 처리기 (GlobalExceptionHandler)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 로직 상의 커스텀 예외 처리 (예: 데이터 없음)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        ErrorResponse response = new ErrorResponse("NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 입력값 검증 실패 처리 (Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        ErrorResponse response = new ErrorResponse("INVALID_INPUT", "입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(response);
    }

    // 그 외 예상치 못한 모든 서버 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception e) {
        // 로깅 작업 수행 (실무 필수)
        log.error("Unhandled Exception occurred: ", e);
        ErrorResponse response = new ErrorResponse("SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

## 3. 실무 가이드 및 주의사항

### 1) 가장 구체적인 예외가 먼저 실행됨
만약 `IllegalArgumentException`과 `Exception`에 대한 핸들러가 둘 다 있다면, 더 구체적인 타입인 `IllegalArgumentException` 핸들러가 우선적으로 선택됨

### 2) 로깅(Logging)의 중요성
`@ExceptionHandler`로 예외를 잡아버리면 로그에 남지 않고 조용히 처리될 수 있음. 운영 환경에서의 추적을 위해 **반드시 에러 로그를 남기는 로직을 포함**해야 함

### 3) 상태 코드(HttpStatus) 선택 기준
*   **400 (Bad Request)**: 클라이언트의 요청 데이터(파라미터)가 잘못된 경우
*   **401 (Unauthorized)**: 인증이 필요한 서비스에 비로그인 사용자가 접근한 경우
*   **403 (Forbidden)**: 로그인은 했지만 해당 리소스에 권한이 없는 경우
*   **404 (Not Found)**: 요청한 리소스(ID 등)가 DB에 없는 경우
*   **500 (Internal Server Error)**: 서버 내부 로직 결함이나 DB 장애 등 클라이언트가 해결할 수 없는 경우

### 요약
> **`@ExceptionHandler`는 예외 발생 시의 흐름을 개발자가 제어할 수 있게 해주는 강력한 도구.**
> 실무에서는 `@RestControllerAdvice`와 함께 사용하여 애플리케이션 전역의 에러 응답 형식을 통일하고, 비즈니스 로직과 에러 처리 로직을 깔끔하게 분리하는 것이 핵심
