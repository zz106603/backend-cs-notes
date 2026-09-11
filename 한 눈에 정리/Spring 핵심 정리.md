---
title: "Spring 핵심 정리"
tags:
  - "Spring"
---
# Spring 핵심 정리

## Spring과 Spring Boot

### Spring
Java 애플리케이션을 만들기 위한 Framework
- 객체 생성과 관리
- 의존성 주입
- Web 요청 처리
- Transaction 관리
- AOP
- Database 연동
> 객체를 직접 만들고 연결하는 일을 Spring Container가 대신 관리

### Spring Boot
Spring 애플리케이션을 더 쉽게 구성하고 실행할 수 있도록 만든 구조

기존 Spring에서는
- Servlet Container 설정
- 각종 Bean 설정
- 외부 Library 설정
등을 직접 해야 하는 부분이 많은데 Spring Boot는 이를 자동 설정해줌
```
Spring Boot
   ↓
Auto Configuration
   ↓
필요한 Spring 설정 자동 구성
```
또한 Tomcat 같은 Web Server를 APplication 내부에 포함해 실행할 수 있음

## IoC와 DI

### IoC(Inversion Of Control)
**객체 생성과 관리의 제어권을 개발자가 아니라 Spring이 가지는 것**

Spring을 사용하지 않는다면 직접 객체를 생성하고 연결을 개발자가 직접 담당
```Java
UserRepository repository = new UserRepository();
UserService service = new UserService(repository);
```

```Java
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```
```
Spring Container
      ↓
UserRepository 생성
      ↓
UserService 생성
      ↓
UserRepository 주입
```
Spring이 필요한 객체를 만들어 연결해서 객체 관리의 제어권이 Spring으로 넘어감

### DI(Dependency Injection)
**객체가 필요로 하는 다른 객체를 외부에서 넣어주는 것**

`UserService`가 직접 `UserRepository`를 만들지 않음
```Java
public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```
```
UserService
   ↓ 필요함
UserRepository

Spring
  ↓
 주입
```
**IoC가 더 큰 개념이고, DI는 IoC를 구현하는 대표적인 방법**

## Bean과 Spring Container
Spring이 생성하고 관리하는 객체를 **Bean**이라고 함

```Java
@Service
public class UserService { }
```
```Java
@Repository
public class UserRepository { }
```
이 객체들은 Spring Container가 관리
```
Spring Container
├─ UserController Bean
├─ UserService Bean
└─ UserRepository Bean
```

### Bean 등록
```Java
@Component
@Service
@Repository
@Controller
@RestController
...
```
Spring Boot가 Application을 시작할 때 Component Scan을 통해 이런 Class를 찾아 Bean으로 등록
```
Application 실행
      ↓
Component Scan
      ↓
@Component 계열 Class 발견
      ↓
Bean 생성
      ↓
Spring Container 등록
```

## Singlton과 Bean Scope
Spring Bean의 기본 Scope은 **Singelton**

Application 안에서 Bean 하나를 만들어 여러 곳에서 공유함
```
Request A ─┐
            ↓
       UserService
            ↑
Request B ─┘
```
```Java
@Service
public class UserService {
    private Long currentUserId;
}
```
- 여러 Thread가 같은 Bean을 사용하기 때문에 `currentUserId`값이 섞일 수 있음
- 그래서 일반적인 Service는 **Stateless 구조**로 만드는 것이 중요

## Spring MVC 요청 처리 흐름
```
Client
  ↓
Tomcat
  ↓
Filter
  ↓
DispatcherServlet
  ↓
Interceptor
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
  ↓
Response
```

### Tomcat
Servelt Container

Client의 HTTP 요청을 받아 Java Web Application으로 전달
```
Client
  ↓
HTTP Request
  ↓
Tomcat
  ↓
Spring
```

### Thread Pool
Tomcat은 요청마다 Thread를 무한히 새로 만드는 것이 아니라 미리 만들어둔 Thread Pool을 사용
```
Tomcat Thread Pool

Thread 1
Thread 2
Thread 3
Thread 4
...

Request A → Thread 1
Request B → Thread 2
Request C → Thread 3
```

### Filter
Servlet 앞뒤에서 요청과 응답을 처리할 수 있음
```
Request
  ↓
Filter
  ↓
DispatcherServlet
```
- 인증
- 요청 Logging
- CORS
- Request Wrapper
Filter는 Spring MVC보다 앞쪽에서 동작

### DispatcherServlet
Spring MVC의 핵심 Servlet

요청을 받아 어떤 Controller가 처리할지 찾고 전체 흐름을 조정
```
Request
   ↓
DispatcherServlet
   ↓
어떤 Controller가 처리할지 찾음
```
> Spring MVC 요청의 중앙 관리자

### Controller
실제 HTTP 요청을 받는 부분
```Java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public UserResponse getUser(
            @PathVariable Long id
    ) { ... }
}
```
- 요청값 받기
- 검증
- Service 호출
- 응답 반환

### Service
비즈니스 로직을 처리

```Java
@Service
public class UserService {

    public User getUser(Long id) { ... }
}
```
- 회원 조회
- 주문 생성
- 결제 처리
- 권한 확인 등

### Repository
Database 접근을 담당
```Java
@Repository
public class UserRepository { }

// JPA 사용
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

## DispatcherServlet 내부 흐름
```
HTTP Request
     ↓
DispatcherServlet
     ↓
HandlerMapping
     ↓
Controller 찾기
     ↓
HandlerAdapter
     ↓
Controller 실행
     ↓
Return Value
     ↓
HTTP Response
```

### HandlerMapping
요청 URL과 Method를 보고 어떤 Controller Method가 처리할지 찾음

ex) `GET /users/1`
```Java
@GetMapping("/users/{id}")
public UserResponse getUser(...)
```

### HandlerAdapter
찾은 Controller Method를 실제로 실행할 수 있도록 연결

Spring MVC는 다양한 형태의 Handler를 지원하기 때문에 HandlerAdapter가 중간에서 실행 방식을 맞춰줌

## Filter와 Interceptor

## Filter
Servlet Container 수준에서 동작
```
Client
 ↓
Filter
 ↓
DispatcherServlet
```
Spring MVC에 들어오기 전에 실행
- CORS
- 인증
- Logging
- Request 변환

### Interceptor
Spring MVC 내부에서 Controller 실행 전후에 동작
```
DispatcherServlet
      ↓
Interceptor
      ↓
Controller
```
- 로그인 확인
- 권한 검사
- Controller 호출 Logging

### 차이
```
Filter
→ Servlet 영역

Interceptor
→ Spring MVC 영역
```
```
Client
 ↓
Filter
 ↓
DispatcherServlet
 ↓
Interceptor
 ↓
Controller
```

## AOP와 Proxy
Spring의 Transaction이나 공통 기능을 이해하려면 Proxy 개념이 중요함

### AOP
여러 Class에서 반복되는 공통 기능을 비즈니스 로직과 분리하는 방식
- Logging
- Transaction
- 권한 검사
- 성능 측정

### Proxy
실제 객체 앞에 대신 호출을 받는 객체를 두는 방식
```
Controller
    ↓
Proxy
    ↓
UserService
```
Proxy가 먼저 요청을 받아 부가 기능을 실행
```
Controller
    ↓
  Proxy
    ↓
Transaction 시작
    ↓
실제 Service 호출
    ↓
Transaction 종료
```

## @Transactional
Spring에서 Transaction 사용
```Java
@Transactional
public void transfer() {
    withdraw();
    deposit();
}
```
```
Controller
    ↓
Transaction Proxy
    ↓
Transaction 시작
    ↓
Service Method 실행
    ↓
   성공
    ↓
  Commit
```
```
Service 실행
    ↓
Exception
    ↓
 Rollback
```

### Proxy가 중요한 이유
`@Transactional`이 붙었다고 Method 안에 Transaction 코드가 직접 삽입되는 것이 아님

Spring이 Proxy 객체를 만들어 실제 Bean 호출 앞뒤에서 Transaction을 처리
```
Controller
   ↓
UserService Proxy
   ↓
Transaction 시작
   ↓
실제 UserService
   ↓
Commit / Rollback
```

### 같은 Class 내부 호출 문제
```Java
@Service
public class UserService {

    public void methodA() {
        methodB();
    }

    @Transactional
    public void methodB() {
    }
}
```
- `MethodA()`가 같은 객체 내부에서 `methodB()`를 직접 호출하면 Proxy를 거치지 않을 수 있음

```
외부
 ↓
Proxy
 ↓
methodA()
 ↓
methodB() 직접 호출
``` 
따라서 `methodB()`의 `@Transactional`이 기대대로 적용되지 않는 문제가 발생할 수 있음

이를 **Self Invocation 문제**라고 함

## Transaction Rollback 기준
Spring의 기본 Transaction 설정에서는 일반적으로 
- RuntimeException
- Error

가 발생하면 Rollback

Checked Exception은 기본적으로 Rollback 대상이 아님

필요하다면 직접 설정할 수 있음
```Java
@Transactional(rollbackFor = Exception.class)
```

## Connection Pool과 Spring
Spring Application이 Database에 접근하려면 DB Connection이 필요함

요청마다 새로운 Connection을 만들면 비용이 크기 때문에 Connection Pool을 사용

대표적으로 **HikariCP**를 사용
```
 Spring Boot
     ↓
Connection Pool

[Connection]
[Connection]
[Connection]
[Connection]
     ↓
  Database
```
Repository에서 Query를 실행할 때
```
Connection Pool
     ↓
Connection 빌림
     ↓
SQL 실행
     ↓
Connection 반환
```

## Thread Pool과 Connection Pool의 관계
ex)
```
Tomcat Thread = 200개
DB Connection = 20개
```
HTTP 요청 100개가 동시에 DB를 요청하더라도 DB Connection은 20개밖에 없음
```
20개 요청
→ DB 사용

나머지 요청
→ Connection 대기
```
```
Slow Query
   ↓
Connection 오래 점유
   ↓
Connection Pool 부족
   ↓
Tomcat Thread 대기
   ↓
Thread Pool까지 부족
   ↓
전체 API 느려짐
```

## Spring Bean 생성 과정
```
Application 실행
      ↓
Spring Container 생성
      ↓
Component Scan
      ↓
Bean 대상 Class 탐색
      ↓
   Bean 생성
      ↓
   의존성 주입
      ↓
    초기화
      ↓
Application 실행 준비 완료
```
```Java
@RestController
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }
}
```
```
UserService Bean 생성
       ↓
UserController 생성
       ↓
UserService 주입
```

## 생성자 주입
Spring 에서는 일반적으로 생성자 주입을 권장
```Java
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```
- 필요한 의존성이 명확함
- `final` 사용 가능
- 객체 생성 시점에 필요한 의존성 보장
- Test하기 쉬움

Field Injection보다 의존 관계를 명확하게 표현할 수 있음

## 순환 참조
두 Bean이 서로를 필요로 하면 문제가 발생할 수 있음
```Java
class A {
    A(B b) {}
}

class B {
    B(A a) {}
}
```
이런 구조를 **Circular Dependency**라고 함

보통 설계 자체를 다시 보는 것이 좋음

## Spring MVC와 JSON 응답
Controller가 객체 반환
```Java
@GetMapping("/users/{id}")
public UserResponse getUser(...) {
    return response;
}
```

실제로 HTTP는 Java 객체를 그대로 전송할 수 없어서 JSON 등으로 변환
```
Java Object
     ↓
Message Converter
     ↓
JSON
     ↓
HTTP Response
```
Spring Boot에서는 보통 Jackson이 JSON 변환에 사용됨

## 전체 HTTP 요청 처리 과정
```
Client
  ↓
HTTP Request
  ↓
Tomcat
  ↓
Tomcat Thread Pool에서 Thread 할당
  ↓
Filter
  ↓
DispatcherServlet
  ↓
HandlerMapping
  ↓
Interceptor
  ↓
Controller
  ↓
Service
  ↓
@Transactional Proxy
  ↓
Repository
  ↓
Connection Pool
  ↓
Database
  ↓
결과 반환
  ↓
Java Object
  ↓
JSON 변환
  ↓
HTTP Response
```

## 백엔드 개발자 질문

### IoC란?
- 객체의 생성과 관리에 대한 제어권을 개발자가 직접 가지는 것이 아니라 Spring Container가 담당하는 것

### DI란?
- 객체가 필요로 하는 다른 객체를 직접 생성하지 않고 외부에서 주입받는 방식

### Bean이란?
- Spring Container가 생성하고 관리하는 객체

### Spring Bean은 기본적으로 어떤 Scope인가?
- Singleton
- 하나의 Bean을 여러 요청 Thread가 공유할 수 있음

### Spring MVC 요청은 어떤 순서로 처리되는가?
```
Tomcat
→ Filter
→ DispatcherServlet
→ Interceptor
→ Controller
→ Service
→ Repository
```

### DispatcherServlet은 무슨 역할을 하는가?
- Spring MVC 요청을 받아 적절한 Controller를 찾아 실행하도록 전체 요청 흐름을 조정

### Filter와 Interceptor의 차이는?
- Filter는 Servlet 영역에서 DispatcherServlet 전후로 동작
- Interceptor는 Spring MVC 내부에서 Controller 호출 전후로 동작

### @Transactional은 어떻게 동작하는가?
- Spring이 Proxy를 만들고 실제 Service Method 호출 앞뒤에서 Transaction 시작, Commit, Rollback을 처리

### 같은 Class 내부에서 @Transactional 호출 문제?
- 내부 호출은 Proxy를 거치지 않을 수 있기 때문에 Transaction 기능이 적용되지 않을 수 있음

### Connection Pool을 사용하는 이유?
- 매 요청마다 DB Connection을 새로 생성하는 비용을 줄이고 미리 만들어둔 Connection을 재사용하기 위함

### Tomcat Thread Pool과 Connection Pool은 왜 함께?
- DB Connection이 부족하면 요청 Thread가 대기하고, 대기가 길어지면 Tomcat Thread Pool까지 고갈되어 전체 API가 느려질 수 있기 때문임


## 핵심 흐름
```
1. 객체 관리

Spring Container
      ↓
Bean 생성
      ↓
Dependency Injection
```
```
2. HTTP 요청

Client
 ↓
Tomcat
 ↓
Filter
 ↓
DispatcherServlet
 ↓
Interceptor
 ↓
Controller
```
```
3. 비즈니스 / DB

Controller
 ↓
Service
 ↓
Transaction Proxy
 ↓
Repository
 ↓
Connection Pool
 ↓
Database
```
```
4. 요청과 Thread

HTTP Request
 ↓
Tomcat Thread
 ↓
Singleton Bean
 ↓
여러 Thread가 같은 Bean 사용
 ↓
Stateless 설계 중요
```
