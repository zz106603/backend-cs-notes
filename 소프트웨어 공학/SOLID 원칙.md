# SOLID 원칙 (객체 지향 설계의 5가지 원칙)

객체 지향 설계(OOD)에서 소프트웨어의 유지보수성과 확장성을 높이기 위해 반드시 지켜야 할 5가지 핵심 원칙

---

## 1. SRP (Single Responsibility Principle, 단일 책임 원칙)
**"한 클래스는 하나의 책임만 가져야 한다"**

*   **설명**: 어떤 클래스를 변경해야 하는 이유는 오직 하나뿐이어야 함을 의미함
*   **실무 예시**: `UserService`에서 회원 가입 로직과 로그 파일을 직접 생성하는 로직이 섞여 있다면, 로그 포맷 변경 시 `UserService`를 수정해야 함. 이를 별도의 `LogService`로 분리해야 함

### 코드 예시
```java
// 개선 전: 사용자 관리와 로그 기록 책임을 동시에 가짐
public class UserService {
    public void register(User user) {
        // 회원 가입 로직
        System.out.println("회원 가입 처리");
        // 로그 기록 로직 (SRP 위반)
        System.out.println("로그 기록: " + user.getName() + " 가입");
    }
}

// 개선 후: 책임을 분리
public class UserService {
    private final LogService logService;
    public void register(User user) {
        System.out.println("회원 가입 처리");
        logService.log(user.getName() + " 가입");
    }
}

public class LogService {
    public void log(String message) {
        System.out.println("로그 기록: " + message);
    }
}
```

---

## 2. OCP (Open-Closed Principle, 개방-폐쇄 원칙)
**"소프트웨어 요소는 확장에는 열려 있으나 변경에는 닫혀 있어야 한다"**

*   **설명**: 기존 코드를 수정하지 않고도 새로운 기능을 추가할 수 있도록 설계해야 함을 의미함
*   **실무 예시**: 새로운 결제 수단(Apple Pay)을 추가할 때, 기존 결제 로직(`PaymentService`)을 수정하는 대신 `Payment` 인터페이스를 구현하는 새로운 클래스를 만드는 방식

### 코드 예시
```java
// 개선 전: 새로운 결제 수단 추가 시 기존 로직 수정 필요 (OCP 위반)
public class PaymentService {
    public void process(String type) {
        if (type.equals("Card")) { /* 카드 결제 */ }
        else if (type.equals("Cash")) { /* 현금 결제 */ }
    }
}

// 개선 후: 인터페이스를 통한 확장
public interface Payment {
    void pay();
}

public class CardPayment implements Payment { public void pay() { /* 카드 결제 */ } }
public class ApplePayPayment implements Payment { public void pay() { /* 애플페이 결제 */ } }

public class PaymentService {
    public void process(Payment payment) {
        payment.pay(); // 기존 코드 수정 없이 결제 수단 확장 가능
    }
}
```

---

## 3. LSP (Liskov Substitution Principle, 리스코프 치환 원칙)
**"자식 클래스는 언제나 부모 클래스를 대체할 수 있어야 한다"**

*   **설명**: 상속 관계에서 부모 객체의 자리에 자식 객체를 넣어도 프로그램의 논리적 흐름이 깨지지 않아야 함을 의미함
*   **실무 예시**: `Rectangle`(직사각형) 클래스를 상속받은 `Square`(정사각형) 클래스가 부모의 `setWidth()`와 `setHeight()`의 기대를 저버리는 경우(둘 중 하나만 바꿔도 둘 다 바뀌는 등)는 LSP 위반임

### 코드 예시
```java
// 개선 전: 정사각형이 직사각형을 상속받아 부모의 행동을 왜곡함 (LSP 위반)
public class Rectangle {
    protected int width, height;
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) { this.width = this.height = w; }
    @Override
    public void setHeight(int h) { this.width = this.height = h; }
}

// 개선 후: 상속 대신 인터페이스나 별도 클래스로 분리
public interface Shape {
    int getArea();
}
```

---

## 4. ISP (Interface Segregation Principle, 인터페이스 분리 원칙)
**"자신이 사용하지 않는 메서드에 의존하도록 강제해서는 안 된다"**

*   **설명**: 범용적인 큰 인터페이스 하나보다, 클라이언트의 목적에 맞는 구체적인 여러 개의 인터페이스로 쪼개는 것이 좋다는 원칙
*   **실무 예시**: 스마트 장치 인터페이스(`SmartDevice`)에 `print()`, `fax()`, `scan()`이 다 있다면, 스캐너 기능만 필요한 기기도 불필요한 메서드를 구현해야 함. 이를 `Printer`, `Scanner`, `Fax` 인터페이스로 분리해야 함

### 코드 예시
```java
// 개선 전: 하나의 거대한 인터페이스 (ISP 위반)
public interface SmartDevice {
    void print();
    void fax();
    void scan();
}

// 개선 후: 인터페이스 분리
public interface Printer { void print(); }
public interface Scanner { void scan(); }

public class PhotoScanner implements Scanner {
    public void scan() { /* 스캔만 구현 */ }
}
```

---

## 5. DIP (Dependency Inversion Principle, 의존역전 원칙)
**"추상화에 의존해야 하며, 구체화에 의존해서는 안 된다"**

*   **설명**: 구현체(고수준 모듈)가 아닌 인터페이스나 추상 클래스(저수준 모듈)에 의존하도록 설계해야 함을 의미함
*   **실무 예시**: `OrderService`가 `MySqlRepository`를 직접 참조하는 대신, `Repository` 인터페이스를 참조하게 하고 외부에서 구현체를 주입(DI)받는 방식

### 코드 예시
```java
// 개선 전: 고수준 모듈이 저수준 모듈의 구현체에 직접 의존 (DIP 위반)
public class OrderService {
    private MySqlRepository repository = new MySqlRepository();
}

// 개선 후: 인터페이스에 의존하고 외부에서 주입받음
public class OrderService {
    private final Repository repository; // 인터페이스 의존

    public OrderService(Repository repository) {
        this.repository = repository;
    }
}
```

---

## 6. 요약 및 실무 가이드

| 원칙 | 핵심 키워드 | 목적 |
| :--- | :--- | :--- |
| **SRP** | 단일 책임 | 낮은 결합도, 높은 응집도 |
| **OCP** | 확장성 | 변화에 유연하게 대응 |
| **LSP** | 다형성 | 상속 구조의 안정성 보장 |
| **ISP** | 구체화 | 클라이언트 맞춤형 인터페이스 |
| **DIP** | 추상화 | 구현 기술 변경 시 영향 최소화 |

### 결론
> **SOLID 원칙은 단순히 코드를 예쁘게 짜는 것이 아니라, "변화에 강한 소프트웨어"를 만들기 위한 약속임.**
> 실무에서는 이 원칙들을 기계적으로 적용하기보다, 프로젝트의 규모와 도메인의 복잡도를 고려하여 적절한 수준의 추상화와 설계를 선택하는 것이 중요함
