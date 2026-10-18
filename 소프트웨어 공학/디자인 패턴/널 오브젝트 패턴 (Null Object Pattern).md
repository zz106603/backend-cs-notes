# 널 오브젝트 패턴 (Null Object Pattern)

객체가 없을 때 `null`을 반환하는 대신, **"아무 일도 하지 않는 객체"** 를 반환하여 `null` 체크 로직을 없애는 디자인 패턴

---

## 1. 왜 필요한가요? (문제 상황)

### 상황: 고객 등급별 할인 정책
쇼핑몰에서 고객 등급(`Grade`)에 따라 할인을 적용하려고 함. 그런데 등급이 없는 비회원 고객도 있음

### Bad: 반복적인 null 체크
```java
public double calculateDiscount(Customer customer, double price) {
    DiscountPolicy policy = customer.getDiscountPolicy();
    
    // policy가 null인지 매번 확인해야 함 (귀찮음 + 실수하기 쉬움)
    if (policy != null) {
        return policy.applyDiscount(price);
    } else {
        return price; // 할인 없음
    }
}
```
*   **문제점**: `DiscountPolicy`를 사용하는 모든 곳에서 `if (policy != null)` 코드를 반복해서 작성해야 함. 만약 한 곳이라도 빼먹으면 `NullPointerException`이 발생하여 서버가 죽을 수 있음

---

## 2. 해결 방법 (Null Object 적용)

"할인 정책이 없다"는 것을 `null`로 표현하지 말고, **"할인을 0원 해주는 정책 객체"** 를 만들어서 대신 사용함

### 1) 인터페이스 정의
```java
interface DiscountPolicy {
    double applyDiscount(double price);
}
```

### 2) 실제 객체 (VIP 할인)
```java
class VipDiscountPolicy implements DiscountPolicy {
    @Override
    public double applyDiscount(double price) {
        return price * 0.9; // 10% 할인
    }
}
```

### 3) 널 객체 (할인 없음)
```java
class NullDiscountPolicy implements DiscountPolicy {
    @Override
    public double applyDiscount(double price) {
        return price; // 아무것도 하지 않음 (할인율 0%)
    }
}
```

### Good: 깔끔해진 코드
```java
public double calculateDiscount(Customer customer, double price) {
    // customer.getDiscountPolicy()는 절대 null을 반환하지 않음
    // (없으면 NullDiscountPolicy 객체를 반환하도록 구현됨)
    DiscountPolicy policy = customer.getDiscountPolicy();
    
    // null 체크 없이 바로 메서드 호출 가능
    return policy.applyDiscount(price);
}
```
```java
public class Customer {
    private String grade; // 고객 등급 ("VIP", "GOLD" 또는 null)

    public Customer(String grade) {
        this.grade = grade;
    }

    /**
     * 고객 등급에 맞는 할인 정책 객체를 반환함
     * 이 메서드는 절대 null을 반환하지 않음
     */
    public DiscountPolicy getDiscountPolicy() {
        if ("VIP".equals(this.grade)) {
            // 등급이 VIP이면 VipDiscountPolicy 인스턴스를 반환
            return new VipDiscountPolicy(); 
        }
        // (추가) 등급이 GOLD이면 GoldDiscountPolicy 인스턴스를 반환
        // else if ("GOLD".equals(this.grade)) {
        //     return new GoldDiscountPolicy();
        // }
        
        // 등급이 없거나(null) 해당 등급의 정책이 없으면,
        // "아무 할인도 하지 않는" NullDiscountPolicy 인스턴스를 반환
        return new NullDiscountPolicy();
    }
}
```

---

## 3. 실무 예시: 로깅 (Logging)

실무에서 가장 흔하게 볼 수 있는 예시는 로깅 라이브러리

*   **상황**: 개발 환경에서는 로그를 찍고 싶지만, 운영 환경에서는 성능 때문에 로그를 끄고 싶음
*   **적용**:
    *   개발 환경: `ConsoleLogger` (콘솔에 출력하는 진짜 객체) 주입
    *   운영 환경: `NullLogger` (메서드 내부가 비어있는 객체) 주입
*   **효과**: 비즈니스 로직(`service.doSomething()`)에서는 `logger.info("...")`를 호출하지만, `NullLogger`가 주입된 경우 아무 일도 일어나지 않음. 코드를 수정할 필요 없이 설정만으로 동작을 제어할 수 있음

---

## 4. 장단점

### 장점
*   **코드 간결성**: 지저분한 `if (obj != null)` 체크 로직이 사라져 코드가 매우 깔끔해짐
*   **안정성**: `NullPointerException` 발생 가능성을 원천 차단함

### 단점 및 주의사항
*   **디버깅의 어려움**: 예외가 발생해야 할 상황(데이터 누락 등)에서도 아무 일도 안 하고 조용히 넘어가기 때문에, 나중에 원인을 찾기 어려울 수 있음
*   **클래스 폭발**: 널 객체를 위해 별도의 클래스를 만들어야 하므로 클래스 개수가 늘어남

> **결론**: `null`일 때 "아무것도 안 해도 되는 경우"나 "기본 동작(Default Behavior)이 명확한 경우"에만 사용하는 것이 좋음. 무조건적인 사용은 오히려 독이 될 수 있음
