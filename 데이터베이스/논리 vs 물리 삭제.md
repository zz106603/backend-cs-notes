# 논리 삭제(Soft Delete) vs 물리 삭제(Hard Delete)

데이터를 삭제할 때, 실제로 DB에서 지울지(물리) 아니면 "삭제됨" 표시만 남겨둘지(논리) 결정하는 전략

---

## 1. 두 방식의 차이점

### 1) 물리 삭제 (Hard Delete)
*   **개념**: SQL의 `DELETE` 명령어를 사용하여 데이터를 **영구적으로 삭제**
*   **실무 예시**:
    *   **개인정보 파기**: 법적 보관 기간(예: 3년)이 지난 회원의 개인정보는 복구 불가능하게 지워야 함
    *   **임시 데이터**: 인증 번호, 장바구니 임시 저장 등 수명이 짧고 이력이 필요 없는 데이터
*   **장점**: 저장 공간(Disk)을 확보할 수 있고, 테이블 크기가 줄어들어 조회 성능이 유지됨
*   **단점**: 한 번 지우면 복구가 불가능함 (백업본이 없다면)

### 2) 논리 삭제 (Soft Delete)
*   **개념**: SQL의 `UPDATE` 명령어를 사용하여 `deleted_at`이나 `is_deleted` 같은 컬럼을 업데이트함. 데이터는 그대로 남아있지만, 애플리케이션에서는 삭제된 것처럼 취급
*   **실무 예시**:
    *   **회원 탈퇴**: 사용자가 "탈퇴" 버튼을 눌러도, 혹시 모를 변심이나 CS 처리를 위해 30일간 데이터를 남겨둠
    *   **주문 취소**: 주문을 취소했다고 해서 주문 이력 자체가 사라지면 안 됨 (매출 통계, 환불 내역 등에 필요)
*   **장점**: 데이터 복구가 매우 쉽고(UPDATE만 다시 하면 됨), 과거 데이터를 분석(이력 관리)하는 데 활용할 수 있음
*   **단점**: 데이터가 계속 쌓여서 테이블이 비대해지고, 조회 쿼리마다 `WHERE deleted = false` 조건을 매번 붙여야 하는 번거로움이 있음

---

## 2. 실무에서의 선택 가이드

| 상황 | 추천 방식 | 이유 |
| :--- | :--- | :--- |
| **중요한 비즈니스 데이터** (주문, 결제, 회원) | **논리 삭제** | CS 대응, 통계, 감사(Audit) 등을 위해 이력이 필수적임 |
| **법적 파기 의무 데이터** (오래된 개인정보) | **물리 삭제** | 개인정보보호법 준수를 위해 완전히 지워야 함 |
| **단순 로그, 임시 데이터** | **물리 삭제** | 데이터 가치가 낮고 양이 많으므로 삭제하여 용량 확보 필요 |
| **관계(Relation)가 복잡한 데이터** | **논리 삭제** | 부모 데이터를 물리 삭제하면 자식 데이터(Foreign Key) 처리가 복잡해짐 (Cascade 이슈) |

---

## 3. JPA에서의 활용법 (Soft Delete 구현)

JPA(Hibernate) 기능을 사용하면 논리 삭제를 마치 물리 삭제처럼 편리하게 다룰 수 있음

### 1) 엔티티 설정
```java
@Entity
@SQLDelete(sql = "UPDATE member SET deleted = true WHERE id = ?") // (1) 삭제 시 UPDATE 실행
@Where(clause = "deleted = false") // (2) 조회 시 자동으로 삭제 안 된 것만 필터링
public class Member {
    
    @Id @GeneratedValue
    private Long id;
    
    private String name;
    
    private boolean deleted = false; // 삭제 여부 플래그
}
```

### 2) 사용 코드
```java
// 삭제 (애플리케이션 코드는 그대로 delete를 호출)
memberRepository.delete(member); 
// -> 실제로는 UPDATE member SET deleted = true ... 쿼리가 나감

// 조회 (애플리케이션 코드는 그대로 findAll을 호출)
List<Member> members = memberRepository.findAll();
// -> 실제로는 SELECT ... FROM member WHERE deleted = false 쿼리가 나감
```

### 주의사항
*   `@Where`는 글로벌하게 적용되므로, **삭제된 데이터까지 포함해서 조회해야 하는 관리자 페이지** 등을 개발할 때 불편할 수 있음
*   이런 경우에는 `@Where`를 쓰지 않고, JPQL(`select m from Member m`)을 직접 작성하거나 `findAllByDeletedFalse()` 같은 메서드를 명시적으로 사용하는 것이 더 유연할 수 있음
