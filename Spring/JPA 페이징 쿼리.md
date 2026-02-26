# JPA 페이징 쿼리 (JPA Pagination)

Spring Data JPA는 데이터베이스의 페이징 처리를 매우 편리하게 추상화하여 제공함

하지만 편리함 뒤에 숨겨진 동작 원리와 성능 이슈를 이해하고 사용해야 함

---

## 1. Pageable 인터페이스와 PageRequest

Spring Data JPA에서는 `Pageable` 인터페이스를 통해 페이징 정보를 캡슐화

### 사용 방법
Repository 메서드의 파라미터로 `Pageable`을 넘기면, JPA가 알아서 페이징 쿼리를 생성함

```java
// Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 반환 타입이 Page<T>이면 페이징 결과와 함께 전체 카운트 쿼리도 실행됨
    Page<Member> findByAge(int age, Pageable pageable);
}

// Service
public void getMembers() {
    // 0페이지부터 시작, 한 페이지에 10개씩, ID 내림차순 정렬
    PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
    
    Page<Member> page = memberRepository.findByAge(20, pageRequest);
    
    List<Member> members = page.getContent(); // 조회된 데이터
    int totalPages = page.getTotalPages();    // 전체 페이지 수
    long totalElements = page.getTotalElements(); // 전체 데이터 수
}
```

### 반환 타입에 따른 차이
*   **`Page<T>`**: 조회된 데이터 + 전체 데이터 수(Count 쿼리 실행 O). 게시판 하단 페이징 처리에 적합
*   **`Slice<T>`**: 조회된 데이터 + 다음 페이지 존재 여부(Count 쿼리 실행 X). 모바일 '더 보기'나 무한 스크롤에 적합. `LIMIT + 1`을 조회하여 다음 페이지가 있는지 확인하는 방식
*   **`List<T>`**: 단순히 데이터만 조회. 페이징 정보 불필요 시 사용

---

## 2. JPA 페이징의 한계 (OFFSET 방식)

`PageRequest`를 사용하면 기본적으로 DB의 `OFFSET`, `LIMIT` 구문을 사용함

```sql
-- PageRequest.of(1000, 10) 요청 시 실행되는 쿼리 (MySQL 기준)
SELECT * FROM member 
WHERE age = 20 
ORDER BY id DESC 
LIMIT 10 OFFSET 10000;
```

### 성능 이슈
*   RDB 페이징 쿼리 문서에서 다룬 것처럼, **페이지 번호가 뒤로 갈수록 `OFFSET` 값이 커져 성능이 급격히 저하**됨
*   JPA가 편리하게 쿼리를 만들어주지만, 근본적인 DB 성능 문제를 해결해주지는 않음

---

## 3. JPA에서 No-Offset (Keyset Pagination) 구현하기

대용량 데이터 처리를 위해 No-Offset 방식을 사용하려면, `Pageable`에 의존하지 않고 **직접 쿼리를 작성**해야 함

주로 **QueryDSL**을 사용하여 구현합니다.

### QueryDSL을 활용한 구현 예시

```java
public List<Member> findMembersNoOffset(Long lastMemberId, int pageSize) {
    return queryFactory
            .selectFrom(member)
            .where(
                // lt: less than (<)
                // 첫 페이지 요청(lastMemberId가 null)일 때는 조건 무시
                ltMemberId(lastMemberId), 
                member.age.eq(20)
            )
            .orderBy(member.id.desc())
            .limit(pageSize)
            .fetch();
}

// 동적 쿼리 처리를 위한 메서드
private BooleanExpression ltMemberId(Long memberId) {
    if (memberId == null) {
        return null; // WHERE 절에서 무시됨
    }
    return member.id.lt(memberId); // id < lastMemberId
}
```

### 핵심 포인트
1.  **`Pageable` 미사용**: `OFFSET`을 쓰지 않으므로 `Pageable` 파라미터 대신 `lastId`와 `pageSize`를 직접 받음
2.  **동적 쿼리**: 첫 페이지 조회 시에는 `lastId`가 없으므로 `WHERE` 조건에서 제외해야 함. QueryDSL의 `BooleanExpression`을 사용하면 이를 깔끔하게 처리할 수 있음
3.  **인덱스 활용**: `WHERE id < ?` 조건은 PK 인덱스를 바로 탈 수 있어 매우 빠름


---

## 4. Count 쿼리 최적화

`Page<T>`를 반환하면 JPA는 자동으로 `count(*)` 쿼리를 실행함. 데이터가 많으면 이 Count 쿼리 자체가 큰 부하가 됨

### 해결 방법
1.  **Count 쿼리 분리**: 복잡한 조인이 있는 목록 쿼리와 달리, Count 쿼리는 조인을 줄이거나 최적화할 수 있는 경우가 많음. `@Query`의 `countQuery` 속성을 사용하여 별도로 작성함
    ```java
    @Query(value = "select m from Member m left join m.team t where ...",
           countQuery = "select count(m) from Member m where ...") // 조인 없이 카운트
    Page<Member> findMembers(Pageable pageable);
    ```
2.  **Slice 사용**: 전체 페이지 수가 필요 없는 UI(무한 스크롤 등)라면 `Page` 대신 `Slice`를 사용하여 Count 쿼리 자체를 제거함
