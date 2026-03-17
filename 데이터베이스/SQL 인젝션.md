# SQL 인젝션 (SQL Injection)

사용자의 입력값이 **데이터(Data)** 로 취급되지 않고, 개발자가 의도하지 않은 **SQL 구문(Code)** 으로 해석되어 데이터베이스를 공격하는 보안 취약점

---

## 1. 공격 원리 및 시나리오

### 문제 상황: 동적 쿼리 (문자열 조합)
가장 기본적인 로그인 로직. 사용자가 입력한 `username`과 `password`를 문자열 그대로 합쳐서 SQL 쿼리를 만듬

```java
// 매우 취약한 코드
String sql = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

### 공격 시나리오 1: 로그인 우회
1.  **공격자 입력**:
    *   `username`: `admin' OR '1'='1' --`
    *   `password`: (아무거나)
2.  **실행되는 SQL**:
    ```sql
    SELECT * FROM users WHERE username = 'admin' OR '1'='1' --' AND password = '...';
    ```
3.  **결과**:
    *   `'admin'` 조건은 거짓일 수 있지만, `OR '1'='1'`은 항상 참(True)
    *   `--`는 주석 처리이므로, 그 뒤의 비밀번호 비교 로직은 무시됨
    *   결과적으로 `WHERE` 절이 항상 참이 되어, 테이블의 첫 번째 사용자인 `admin`으로 로그인이 성공함

### 공격 시나리오 2: 데이터 탈취
1.  **공격자 입력**:
    *   `username`: `' UNION SELECT credit_card_number, NULL FROM credit_cards --`
2.  **실행되는 SQL**:
    ```sql
    SELECT * FROM users WHERE username = '' UNION SELECT credit_card_number, NULL FROM credit_cards --' AND ...
    ```
3.  **결과**:
    *   앞의 `SELECT` 결과와 뒤의 `UNION SELECT` 결과가 합쳐짐
    *   원래 사용자 정보를 보여줘야 할 화면에 다른 테이블인 `credit_cards`의 신용카드 번호가 노출될 수 있음

---

## 2. 방어 방법

### 1) Prepared Statement 사용 (가장 기본적이고 확실한 방법)
`PreparedStatement`는 사용자의 입력값이 들어갈 자리를 `?` (Placeholder)로 미리 정해놓음

```java
// 안전한 코드
String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, username); // 입력값은 여기서 데이터로만 바인딩됨
pstmt.setString(2, password);
ResultSet rs = pstmt.executeQuery();
```

#### 동작 원리: "코드와 데이터의 분리"
1.  **컴파일**: `PreparedStatement`는 `?`가 포함된 SQL 쿼리의 **구조(뼈대)** 를 먼저 DB에 보내 컴파일함. DB는 이 쿼리가 어떤 작업을 할지 미리 파악함
2.  **데이터 전송**: `setString()` 메서드를 통해 전달된 사용자 입력값은 순수한 **데이터**로만 취급됨. `OR`, `UNION` 같은 SQL 키워드가 포함되어 있어도, 단순한 문자열로 처리될 뿐 SQL 구문으로 해석되지 않음
3.  **결과**: 공격자가 `' OR '1'='1'`을 입력해도, DB는 `username`이 `' OR '1'='1'`이라는 문자열인 사용자를 찾으려고 할 뿐, 논리 연산을 수행하지 않음

### 2) ORM 사용 (JPA, Hibernate, MyBatis)
현대적인 ORM 프레임워크는 내부적으로 `PreparedStatement`를 사용하여 쿼리를 생성하므로, 대부분의 SQL 인젝션 공격을 자동으로 방어해줌

```java
// JPA 예시 (안전)
List<User> users = em.createQuery("select u from User u where u.username = :username", User.class)
                     .setParameter("username", username)
                     .getResultList();
```
> **주의**: JPQL이나 MyBatis에서 파라미터 바인딩(`:username`, `#{username}`)을 사용하지 않고, 문자열을 직접 더하는 방식(`+`)으로 쿼리를 만들면 똑같이 SQL 인젝션에 취약해짐

### 3) 기타 방어 계층
*   **입력값 검증 (Input Validation)**: 사용자 입력값에 허용되지 않는 특수문자(`'`, `--`, `;` 등)가 포함되어 있는지 서버단에서 검증함
*   **최소 권한 원칙**: 웹 애플리케이션이 사용하는 DB 계정에 `DROP TABLE` 같은 위험한 권한을 부여하지 않음
*   **에러 메시지 노출 금지**: DB 에러 메시지를 사용자에게 그대로 보여주면, 공격자가 DB 구조에 대한 힌트를 얻을 수 있음
