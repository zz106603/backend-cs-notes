# Statement vs PreparedStatement

JDBC에서 SQL을 실행하기 위해 사용하는 두 인터페이스의 차이점을 성능, 보안, 가독성 측면에서 비교

---

## 1. 개념 및 동작 방식

### Statement
SQL 구문을 문자열로 완전하게 구성한 후, DB에 전송하여 실행하는 방식

*   **특징**: 쿼리 실행 시마다 SQL 파싱, 컴파일, 최적화 과정을 매번 수행함
*   **사용 예시**: 동적인 파라미터가 없는 간단한 쿼리 실행 시 사용될 수 있으나, 실무에서는 거의 사용되지 않음

```java
// Statement 사용 (비권장)
String username = "admin";
String sql = "SELECT * FROM users WHERE username = '" + username + "'";

Statement stmt = connection.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

### PreparedStatement
SQL 구문의 틀(Skeleton)을 미리 컴파일해 두고, 실행 시점에 파라미터만 바인딩하여 실행하는 방식

*   **특징**: `?` (Placeholder)를 사용하여 쿼리 구조를 미리 정의함. DB는 이 구조를 캐싱(Caching)하여 재사용하므로 반복 실행 시 성능이 우수함
*   **사용 예시**: 대부분의 실무 로직에서 기본적으로 사용됨

```java
// PreparedStatement 사용 (권장)
String sql = "SELECT * FROM users WHERE username = ?";

PreparedStatement pstmt = connection.prepareStatement(sql);
pstmt.setString(1, "admin"); // 파라미터 바인딩
ResultSet rs = pstmt.executeQuery();
```

---

## 2. 주요 차이점 비교

### 1) 보안성 (SQL Injection 방지)
가장 결정적인 차이점은 **SQL 인젝션 공격에 대한 방어 능력**

*   **Statement**: 사용자 입력값을 단순히 문자열로 연결(`+`)하기 때문에, 악의적인 SQL 구문이 포함되면 그대로 실행됨
    *   예: `username`에 `' OR '1'='1` 입력 시 무조건 로그인 성공
*   **PreparedStatement**: 입력값을 **SQL 문법이 아닌 단순 데이터(리터럴)** 로 취급함
    *   DB 내부적으로 특수 문자를 이스케이프(Escape) 처리하여 안전하게 바인딩하므로 공격을 무력화함

### 2) 성능 (캐싱 활용)
DB는 쿼리를 받으면 `파싱 -> 컴파일 -> 최적화 -> 실행` 단계를 거침

*   **Statement**: 쿼리 문자열이 조금이라도 다르면(예: 파라미터 값 변경) 새로운 쿼리로 인식하여 모든 단계를 다시 수행함
*   **PreparedStatement**: `?` 부분만 다른 동일한 쿼리 구조라면, 이미 컴파일된 실행 계획(Execution Plan)을 **캐시에서 재사용**함. 따라서 반복적인 쿼리 실행 시 오버헤드가 훨씬 적음

### 3) 가독성 및 유지보수
*   **Statement**: 따옴표(`'`)와 더하기 기호(`+`)가 뒤섞여 코드가 복잡해지고 실수하기 쉬움
*   **PreparedStatement**: 쿼리 구조와 데이터 바인딩이 분리되어 코드가 깔끔하고 명확함

---

## 3. 실무 가이드: JPA와 PreparedStatement

현대적인 Java 애플리케이션 개발에서는 JDBC를 직접 사용하기보다 JPA(Hibernate)나 MyBatis 같은 ORM/SQL Mapper를 주로 사용함

**중요한 점은 이러한 프레임워크들이 내부적으로 `PreparedStatement`를 기본값으로 사용한다는 것**

### JPA (Hibernate) 예시
개발자가 JPQL이나 Criteria API를 작성하면, Hibernate는 이를 SQL로 변환할 때 자동으로 `PreparedStatement` 방식으로 파라미터를 바인딩

```java
// JPA Repository 사용 시
// 내부적으로 PreparedStatement가 생성되어 실행됨
User user = userRepository.findByUsername("user1");
```

### 결론
> **실무에서는 보안과 성능, 유지보수 모든 면에서 우수한 `PreparedStatement`를 반드시 사용해야 함**
> 직접 JDBC를 다루지 않더라도, ORM 프레임워크가 이를 기반으로 동작함을 이해하는 것이 중요