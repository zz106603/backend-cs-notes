### 자바에서 클래스 정보 알아내기 (Java Reflection API)

**정의**
- **리플렉션(Reflection)** 은 구체적인 클래스 타입을 알지 못해도, 그 클래스의 메서드, 타입, 변수들에 접근할 수 있도록 해주는 자바 API
- 실행 중인 자바 애플리케이션 내부에서 역으로 클래스의 정보를 들여다본다고 하여 '반사(Reflection)'라고 부름

**주요 기능**
- 런타임에 클래스의 이름만 알고 있다면 해당 클래스의 객체를 생성할 수 있음
- 객체의 필드(변수) 값에 접근하거나 수정할 수 있음 (심지어 `private` 필드도 가능)
- 객체의 메서드를 동적으로 호출할 수 있음

---

### 실무 활용 예시

리플렉션은 일반적인 비즈니스 로직보다는 **프레임워크나 라이브러리 개발**에 주로 사용

#### 1. 스프링 프레임워크의 의존성 주입 (DI)
스프링은 개발자가 만든 클래스(`@Controller`, `@Service`)를 알지 못하지만, 리플렉션을 통해 해당 클래스를 분석하고 객체를 생성하여 관리
```java
// 스프링 내부 동작 예시 (개념적)
Class<?> clazz = Class.forName("com.example.MyService"); // 클래스 정보 로딩
Object instance = clazz.getDeclaredConstructor().newInstance(); // 객체 생성

// @Autowired가 붙은 필드를 찾아 의존성 주입
for (Field field : clazz.getDeclaredFields()) {
    if (field.isAnnotationPresent(Autowired.class)) {
        field.setAccessible(true); // private 접근 허용
        field.set(instance, dependencyInstance); // 값 주입
    }
}
```

#### 2. JSON 파싱 라이브러리 (Jackson, Gson)
JSON 데이터를 자바 객체로 변환할 때, 라이브러리는 자바 객체의 필드 이름을 알 수 없으므로 리플렉션을 사용해 필드에 값을 채워 넣음
```java
// JSON: {"name": "Alice", "age": 20}
// Java Class: class User { private String name; private int age; }

// 라이브러리 내부 동작
User user = new User();
Field nameField = User.class.getDeclaredField("name");
nameField.setAccessible(true);
nameField.set(user, "Alice"); // JSON 값을 필드에 주입
```

#### 3. IDE의 자동완성 기능
IntelliJ나 Eclipse 같은 IDE가 우리가 작성한 클래스의 메서드 목록을 띄워줄 때 리플렉션을 사용하여 정보를 읽어옴

---

### 리플렉션 사용법 (간단 예제)

```java
public class Person {
    private String name = "Unknown";
    public void sayHello() { System.out.println("Hello, " + name); }
}

// 사용 예시
public static void main(String[] args) throws Exception {
    // 1. 클래스 정보 가져오기
    Class<?> clazz = Class.forName("Person");
    
    // 2. 객체 생성하기
    Object person = clazz.getDeclaredConstructor().newInstance();
    
    // 3. private 필드 값 변경하기
    Field field = clazz.getDeclaredField("name");
    field.setAccessible(true); // private 접근 권한 해제
    field.set(person, "Java Developer");
    
    // 4. 메서드 호출하기
    Method method = clazz.getDeclaredMethod("sayHello");
    method.invoke(person); // 출력: Hello, Java Developer
}
```

---

### 주의사항 및 단점

1.  **성능 저하:**
    - 컴파일 타임에 최적화가 이루어지는 일반 코드와 달리, 리플렉션은 런타임에 동적으로 해석되므로 JIT 컴파일러의 최적화를 받기 어렵습니다. 반복적인 호출 시 성능 이슈가 발생할 수 있음
2.  **컴파일 타임 오류 확인 불가:**
    - 존재하지 않는 클래스나 메서드를 호출하려 해도 컴파일러가 잡아주지 못하고, 실행 시점(런타임)에 에러가 발생
3.  **캡슐화 위반:**
    - `setAccessible(true)`를 통해 `private` 멤버에도 접근할 수 있으므로, 객체 지향의 정보 은닉 원칙을 깨뜨릴 수 있음

**결론:** 일반적인 비즈니스 로직 개발 시에는 사용을 지양하고, 프레임워크 개발이나 공통 유틸리티 작성 시에만 신중하게 사용해야 함