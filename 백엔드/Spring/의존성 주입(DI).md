## 의존성 주입 (DI, Dependency Injection)

### 1. 개념
- **의존성(Dependency)**: 객체 A가 기능을 수행하기 위해 객체 B를 필요로 하는 관계 (A -> B)
- **의존성 주입(Injection)**: 객체 내부에서 의존 객체를 직접 생성(`new`)하는 것이 아니라, **외부에서 생성된 객체를 주입받아 사용하는 방식**
- 이를 통해 객체 간의 **결합도(Coupling)** 를 낮추고 **유연성**과 **테스트 용이성**을 높일 수 있음

---

### 2. 왜 필요한가? (Before & After)

#### -- DI 미적용 (강한 결합) --
`BurgerChef`가 `BeefPatty`를 직접 생성하여 사용. 만약 패티를 치킨 패티로 바꾸려면 `BurgerChef` 코드를 직접 수정해야 함

```java
class BurgerChef {
    // 셰프가 특정 패티(BeefPatty)에 강하게 의존함
    private BeefPatty patty = new BeefPatty();

    public void cook() {
        patty.grill();
    }
}
```

#### -- DI 적용 (느슨한 결합) --
`BurgerChef`는 구체적인 패티 대신 `Patty` 인터페이스에 의존. 어떤 패티를 사용할지는 외부에서 결정하여 주입

```java
interface Patty {
    void grill();
}

class BurgerChef {
    private final Patty patty;

    // 외부에서 의존성을 주입받음 (생성자 주입)
    public BurgerChef(Patty patty) {
        this.patty = patty;
    }

    public void cook() {
        patty.grill();
    }
}

// 사용하는 곳 (외부)
Patty beef = new BeefPatty();
BurgerChef chef = new BurgerChef(beef); // 소고기 패티 주입
```
> 이제 `BurgerChef` 코드를 수정하지 않고도 `ChickenPatty`를 주입하여 요리할 수 있음 (OCP 준수)

---

### 3. 의존성 주입 방식 3가지

#### 1) 생성자 주입 (Constructor Injection) - **[권장]**
생성자를 통해 의존성을 주입받는 방식
```java
public class Service {
    private final Repository repository;

    public Service(Repository repository) {
        this.repository = repository;
    }
}
```
- **장점**:
  - **불변성(Immutable)**: 필드를 `final`로 선언하여 객체 생성 시점에 의존성이 확정되고 변하지 않음을 보장
  - **누락 방지**: 컴파일 시점에 의존성 주입 누락을 확인할 수 있음 (생성자 파라미터 강제)
  - **순환 참조 방지**: 스프링 부트 실행 시점에 순환 참조 에러를 미리 잡아낼 수 있음

#### 2) 수정자 주입 (Setter Injection)
Setter 메서드를 통해 의존성을 주입받는 방식
```java
public class Service {
    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
```
- **특징**: 선택적인 의존성이나 변경 가능성이 있는 의존성에 사용
- **단점**: 객체 생성 후 의존성이 주입되지 않은 상태로 메서드가 호출되면 `NullPointerException`이 발생할 수 있음

#### 3) 필드 주입 (Field Injection)
`@Autowired` 등을 사용하여 필드에 바로 주입하는 방식
```java
public class Service {
    @Autowired
    private Repository repository;
}
```
- **단점**: 외부에서 의존성을 변경할 수 없어 테스트하기 어렵고, 프레임워크(Spring)에 강하게 종속됨. **사용을 지양해야 함**

---

### 4. 요약
- **DI의 핵심**: "내가 사용할 객체를 내가 만들지 않고, 남이(외부/프레임워크) 줘서 쓴다."
- **가장 좋은 방법**: **생성자 주입**을 사용하여 불변성을 확보하고 안정적인 객체를 만드는 것이 모범 사례(Best Practice)