# Spring MVC 실행 흐름 (View 응답 vs Message Converter)

Spring MVC는 클라이언트의 요청을 받아 처리하고 응답을 반환하는 과정을 효율적으로 관리함 

이때 컨트롤러의 반환 타입과 어노테이션에 따라 응답 방식이 크게 두 가지로 나뉨

---

## 1. 공통 실행 흐름

모든 HTTP 요청은 프론트 컨트롤러인 **DispatcherServlet**을 거쳐 처리됨

1.  **핸들러 조회**: `HandlerMapping`을 통해 URL에 매핑된 컨트롤러(핸들러)를 찾음
2.  **핸들러 어댑터 조회**: 찾은 컨트롤러를 실행할 수 있는 `HandlerAdapter`를 찾음
3.  **핸들러 실행**: `HandlerAdapter`가 실제 컨트롤러 메서드를 호출하여 비즈니스 로직을 수행함

이후 과정에서 **View를 반환하느냐**, **데이터(JSON/문자열)를 반환하느냐**에 따라 흐름이 달라짐

---

## 2. View 응답 방식 (HTML/JSP 렌더링)

서버에서 HTML을 생성하여 클라이언트에게 전달하는 **SSR(Server Side Rendering)** 방식

### 동작 흐름
1.  **컨트롤러 반환**: 컨트롤러 메서드가 **뷰 이름(String)** 을 반환함
2.  **ViewResolver 호출**: `DispatcherServlet`은 `ViewResolver`에게 뷰 이름을 전달함
3.  **View 객체 생성**: `ViewResolver`는 뷰 이름에 해당하는 실제 `View` 객체를 찾아 반환함
4.  **View 렌더링**: `View` 객체는 모델 데이터를 사용하여 HTML 페이지를 생성(렌더링)하고 응답함

### 실무 예시 코드
```java
@Controller // View를 반환하는 컨트롤러
public class WebController {

    @GetMapping("/hello")
    public String helloPage(Model model) {
        // 비즈니스 로직 수행 후 데이터를 모델에 담음
        model.addAttribute("message", "안녕하세요!");
        
        // "hello"라는 이름의 뷰(hello.html 등)를 찾으라고 전달
        return "hello"; 
    }
}
```

---

## 3. Message Converter 방식 (JSON/REST API)

클라이언트(웹 프론트엔드, 모바일 앱 등)에게 데이터를 직접 JSON 등의 형태로 제공할 때 사용되는 방식

### 동작 흐름
1.  **컨트롤러 반환**: 컨트롤러 메서드에 `@ResponseBody`가 붙어 있거나 클래스가 `@RestController`인 경우, **자바 객체**를 반환함
2.  **HttpMessageConverter 호출**: `DispatcherServlet`은 `ViewResolver` 대신 `HttpMessageConverter`를 호출함
3.  **데이터 직렬화**: 반환된 객체를 클라이언트가 요청한 형식(주로 JSON)으로 변환(직렬화)함 (주로 Jackson 라이브러리 사용)
4.  **HTTP 응답**: 변환된 JSON 데이터를 HTTP 응답 본문에 직접 담아 전송함

### 실무 예시 코드
```java
@RestController // @Controller + @ResponseBody
public class ApiController {

    @GetMapping("/api/data")
    public UserDto getUserData() {
        // 객체를 반환하면 HttpMessageConverter가 JSON으로 자동 변환
        return new UserDto("user1", "서울시 강남구");
    }

    @PostMapping("/api/save")
    public ResponseEntity<String> save(@RequestBody UserDto dto) {
        // ResponseEntity를 사용하여 상태 코드와 함께 데이터 반환 가능
        return ResponseEntity.ok("저장 성공");
    }
}
```

---

## 4. 한눈에 보는 비교표

| 구분 | View 응답 방식 | Message Converter 방식 |
| :--- | :--- | :--- |
| **주요 어노테이션** | `@Controller` | `@RestController` (또는 `@ResponseBody`) |
| **주요 반환값** | `String` (뷰 이름) | 자바 객체 (DTO, Map, List 등) |
| **핵심 컴포넌트** | `ViewResolver` | `HttpMessageConverter` |
| **최종 응답 형태** | HTML 페이지 (View) | JSON/XML/문자열 데이터 (Data) |
| **주요 사용처** | 전통적인 웹 페이지 (SSR) | REST API 서버, 모바일 앱 백엔드 |

### 요약
> **View 응답은 "어떤 페이지를 보여줄 것인가"에 집중하고, Message Converter는 "어떤 데이터를 보낼 것인가"에 집중함.**
> 실무에서는 웹 페이지 서빙은 `ViewResolver`를, 클라이언트와의 데이터 통신은 `HttpMessageConverter`를 사용하여 두 방식을 목적에 맞게 혼용함
