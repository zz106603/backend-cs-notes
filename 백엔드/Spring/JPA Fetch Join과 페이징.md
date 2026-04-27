# JPA Fetch Join과 페이징: 함께 사용하면 안 되는 이유와 해결 방안

JPA를 사용할 때 성능 최적화를 위해 `fetch join`을, 대량 데이터 조회를 위해 `페이징(Paging)`을 사용함

하지만 **일대다(OneToMany) 관계**에서 이 둘을 함께 사용하면 심각한 성능 문제를 일으킬 수 있음

---

## 1. 문제 상황: 메모리 폭탄의 시작

### 시나리오
게시글 목록을 조회하는데, 각 게시글에 달린 댓글들도 함께 가져오고 싶음 (Post-Comment, 1:N 관계)

### X 잘못된 접근
```java
// PostRepository.java
@Query("select p from Post p join fetch p.comments")
Page<Post> findAllWithComments(Pageable pageable);
```
위 코드를 실행하면, JPA는 친절하게 경고 메시지를 로그에 남김
> `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!`

*   **경고의 의미**: "컬렉션(`comments`)을 fetch join 하면서 페이징을 요청했네? DB의 `LIMIT`, `OFFSET`을 쓸 수 없어서, **일단 모든 데이터를 메모리에 다 올린 다음에 애플리케이션에서 잘라줄게!**"
*   **결과**: 만약 게시글이 100만 개라면, 100만 개의 게시글과 관련된 모든 댓글을 DB에서 조회하여 애플리케이션 메모리에 올리려고 시도함. 이는 100% **`OutOfMemoryError`** 로 이어짐

---

## 2. 근본 원인: 왜 DB 페이징이 불가능한가?

### 1) 데이터 뻥튀기 (Data Duplication)
*   `Post` 1개에 `Comment`가 3개 달려있다고 가정
*   `join fetch p.comments` 쿼리를 실행하면, SQL 결과는 아래와 같이 **3줄(Row)** 이 됨. `Post`의 데이터가 댓글 수만큼 중복되어 나타남

| post_id | title | comment_id | content |
| :--- | :--- | :--- | :--- |
| 1 | JPA | 101 | 댓글1 |
| 1 | JPA | 102 | 댓글2 |
| 1 | JPA | 103 | 댓글3 |

### 2) 잘못된 페이징 결과
*   만약 우리가 `PageRequest.of(0, 1)` (첫 페이지에 1개만)을 요청했다면, DB는 위 결과에서 `LIMIT 1`을 적용하여 딱 1줄만 반환함
*   JPA는 이 1줄을 보고 `Post` 객체 1개와 `Comment` 객체 1개를 만듬
*   **문제**: 분명 `Post` 1번은 댓글을 3개 가지고 있는데, 결과적으로는 댓글 1개만 가진 불완전한 객체가 조회됨. **데이터 정합성이 깨짐**

이러한 이유로 JPA는 DB의 `LIMIT`을 믿지 못하고, 모든 데이터를 메모리로 가져와서 직접 페이징하는 위험한 방법을 선택하는 것

---

## 3. 실무 해결 방안

### 해결책 1: 쿼리 분리 (가장 정석적인 방법)
1.  **1단계**: `fetch join` 없이, 페이징만 적용하여 대상 `Post`의 **ID 목록**만 조회
2.  **2단계**: 조회된 ID 목록을 가지고 `IN` 절과 `fetch join`을 사용하여 필요한 모든 데이터를 한 번에 조회

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public Page<PostDto> getPostPage(Pageable pageable) {
        // 1. 페이징을 적용하여 Post 엔티티를 조회 (fetch join 없음)
        Page<Post> postPage = postRepository.findAll(pageable);

        // 2. 조회된 Post의 ID 목록을 추출
        List<Long> postIds = postPage.getContent().stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        // 3. ID 목록으로 fetch join 쿼리를 실행하여 데이터를 한 번에 가져옴
        // (이때는 페이징을 적용하지 않음)
        List<Post> postsWithComments = postRepository.findAllWithCommentsByIds(postIds);
        
        // DTO로 변환하여 반환
        // ...
    }
}

// PostRepository.java
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("select p from Post p join fetch p.comments where p.id in :ids")
    List<Post> findAllWithCommentsByIds(@Param("ids") List<Long> ids);
}
```

### 해결책 2: Batch Size 설정 (더 간단한 방법)
*   `fetch join`을 아예 사용하지 않고, N+1 문제를 **지연 로딩(Lazy Loading)과 Batch Size**로 해결하는 방법
*   **`application.yml`**
    ```yaml
    spring:
      jpa:
        properties:
          hibernate:
            default_batch_fetch_size: 100 # 100개 단위로 IN 쿼리 실행
    ```
*   **동작 방식**:
    1.  `postRepository.findAll(pageable)`을 호출하면, `LIMIT/OFFSET`이 적용된 쿼리가 실행되어 `Post` 10개가 조회됨 (N+1 문제 발생 직전)
    2.  첫 번째 `Post`의 `getComments()`를 호출하는 순간, Hibernate는 나머지 9개 `Post`의 ID까지 모아서 **단 한 번의 `IN` 쿼리**를 실행하여 모든 댓글을 가져옴
    3.  결과적으로 **쿼리가 2번**(`SELECT * FROM post ...`, `SELECT * FROM comment WHERE post_id IN (...)`) 나가지만, 코드가 매우 깔끔하고 직관적

---

## 4. 결론

| 상황 | 해결책 | 장점 | 단점 |
| :--- | :--- | :--- | :--- |
| **일대다(1:N) 페이징** | **쿼리 분리** | 가장 정석적이고 예측 가능한 성능 | 코드가 다소 길어짐 |
| **일대다(1:N) 페이징** | **Batch Size** | 코드가 매우 간결함 | 쿼리가 2번 실행됨 |
| **다대일(N:1) 페이징** | **Fetch Join** | 문제 없음. 그냥 사용하면 됨. | - |

**결론**: 일대다 관계에서 페이징이 필요할 때는 `fetch join`을 쓰지 말고, **쿼리를 분리하거나 Batch Size를 사용**하는 것이 안전하고 올바른 방법
