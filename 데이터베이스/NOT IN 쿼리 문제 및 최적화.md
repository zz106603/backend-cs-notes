# NOT IN 쿼리의 성능 문제와 최적화 (NOT IN vs NOT EXISTS vs LEFT JOIN)

`NOT IN` 쿼리는 특정 조건에 포함되지 않는 데이터를 조회할 때 직관적이고 편리하지만, 대규모 데이터 환경에서 **심각한 성능 저하**와 **데이터 정합성 문제**를 일으킬 수 있음

---

## 1. NOT IN 쿼리의 문제점

### 1) 비효율적인 인덱스 활용 (Full Table Scan)
`NOT IN`은 부정형 조건(`!=`)이기 때문에, 데이터베이스 옵티마이저가 인덱스를 효율적으로 사용하기 어려움

```sql
-- 비효율적인 쿼리
SELECT * FROM posts WHERE id NOT IN (1, 2, 3, ...);
```

*   **동작 원리**:
    *   `id != 1` AND `id != 2` AND `id != 3` ... 조건을 모두 만족하는지 확인해야 함
    *   결국 테이블의 모든 레코드를 하나씩 확인하는 **전체 테이블 스캔(Full Table Scan)** 이나 **인덱스 전체 스캔(Index Full Scan)** 이 발생할 확률이 높음
*   **비교**: `IN` 절은 `OR` 조건(`=`)으로 해석되어 **인덱스 레인지 스캔(Index Range Scan)** 을 통해 빠르게 데이터를 찾을 수 있음

### 2) NULL 값으로 인한 논리적 오류
서브쿼리 결과에 `NULL`이 포함되면, 쿼리 전체가 **항상 비어있는 결과(Empty Set)** 를 반환하는 치명적인 문제가 발생함

```sql
-- 의도치 않은 결과 발생
SELECT * FROM users WHERE status NOT IN ('ACTIVE', 'PENDING', NULL);
```

*   **SQL 해석**: `status != 'ACTIVE'` AND `status != 'PENDING'` AND `status != NULL`
*   **문제**:
    *   SQL에서 `NULL`과의 비교 연산(`!= NULL`)은 항상 **UNKNOWN(알 수 없음)** 이 됨
    *   `UNKNOWN`은 `TRUE`가 아니므로, `WHERE` 절의 조건이 성립하지 않아 어떤 데이터도 반환되지 않음
    *   개발자가 예상하기 힘든 버그의 원인이 됨

---

## 2. 최적화 방안

### 1) NOT EXISTS 활용 (가장 권장)
`NOT EXISTS`는 서브쿼리의 조건을 만족하는 행이 **존재하지 않음**을 확인하는 방식

```sql
-- 최적화된 쿼리 (NOT EXISTS)
SELECT p.*
FROM posts p
WHERE NOT EXISTS (
    SELECT 1
    FROM post_likes pl
    WHERE pl.user_id = 100 AND pl.post_id = p.id
);
```

*   **동작 원리**:
    1.  외부 쿼리(`posts`)의 레코드를 하나씩 가져옴
    2.  서브쿼리(`post_likes`)에서 조건을 만족하는 행이 있는지 확인함
    3.  조건을 만족하는 행을 **하나라도 발견하면 즉시 검사를 중단(Early Exit)** 하고 다음 레코드로 넘어감
*   **장점**:
    *   **인덱스 활용**: 서브쿼리 내의 조인 조건(`pl.post_id = p.id`)이 인덱스를 탈 수 있어 매우 빠름
    *   **NULL 안전성**: `EXISTS`는 `NULL` 값을 논리적으로 올바르게 처리함 (존재 여부만 판단)

### 2) LEFT JOIN + IS NULL 패턴
`LEFT JOIN`을 수행한 후, 조인에 실패한(매칭되지 않은) 행만 필터링하는 방식

```sql
-- 최적화된 쿼리 (LEFT JOIN)
SELECT p.*
FROM posts p
LEFT JOIN post_likes pl ON p.id = pl.post_id AND pl.user_id = 100
WHERE pl.id IS NULL;
```

*   **동작 원리**:
    1.  `posts` 테이블을 기준으로 `post_likes`와 조인함 (`user_id = 100` 조건 포함)
    2.  매칭되는 데이터가 없으면 `post_likes` 쪽 컬럼은 `NULL`로 채워짐
    3.  `WHERE pl.id IS NULL` 조건을 통해 매칭되지 않은(좋아요를 누르지 않은) 게시물만 남김
*   **장점**:
    *   **직관적인 실행 계획**: 조인 연산은 DBMS가 최적화하기 매우 좋음
    *   **대용량 처리**: 특정 상황(서브쿼리 결과가 매우 작을 때)에서는 `NOT EXISTS`보다 유리할 수 있음

---

## 3. 실무 적용 가이드

### 상황별 추천 방법

| 구분 | NOT IN | NOT EXISTS | LEFT JOIN |
| :--- | :--- | :--- | :--- |
| **성능** |  **매우 느림** (Full Scan) |  **빠름** (Index Range Scan) |  **빠름** (Join Optimization) |
| **NULL 처리** |  **위험** (결과 누락 가능) |  **안전** |  **안전** |
| **가독성** |  좋음 |  보통 |  보통 |
| **추천** | 데이터가 매우 적고 NULL이 없음이 확실할 때만 | **대부분의 상황에서 1순위 추천** | 조인 대상이 적거나 특정 DB 특성에 맞춰 튜닝할 때 |

### 요약
> **실무에서는 `NOT IN` 사용을 지양하고, 기본적으로 `NOT EXISTS`를 사용하는 습관을 들이는 것이 좋음.**
> 복잡한 조인이 필요한 경우에는 `LEFT JOIN` 방식을 고려해 볼 수 있음