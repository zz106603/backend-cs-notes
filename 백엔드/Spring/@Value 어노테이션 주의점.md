# @Value 어노테이션 특징 및 주의점

Spring 환경 변수(`application.yml`, `properties`)에 설정된 값을 빈(Bean)의 필드나 생성자 인자에 주입할 때 사용하는 어노테이션

---

## 1. 사용 시 주요 주의사항

### 1) Spring Bean으로 관리되어야만 작동함
`@Value`는 Spring 컨테이너가 빈을 생성하고 의존성을 주입하는 시점에 동작함
*   **실수**: 일반 클래스에서 `new` 키워드로 직접 객체를 생성하면 `@Value`는 동작하지 않고 필드는 `null`이 됨
*   **해결**: 반드시 `@Component`, `@Service` 등으로 등록된 빈 내부에서 사용해야 함

### 2) static 필드에는 주입 불가
Spring은 인스턴스 필드에 값을 주입하므로, `static` 키워드가 붙은 필드에는 직접 `@Value`를 사용할 수 없음 (Setter 주입을 통한 우회는 가능하나 비권장)

### 3) 주입 시점과 NPE (NullPointerException)
필드 주입 방식 사용 시, **생성자 내부**에서 해당 필드를 사용하려고 하면 아직 값이 주입되지 않아 NPE가 발생할 수 있음
*   **해결**: 생성자 시점에 값이 필요하다면 **생성자 주입** 방식을 사용해야 함

---

## 2. 주입 방식별 실무 예시

### 1) 필드 주입 (가장 흔하지만 비권장)
```java
@Component
public class MyService {
    @Value("${api.key}")
    private String apiKey; // 테스트 시 직접 값 변경이 어려움
}
```

### 2) 생성자 주입 (권장 방식)
Spring 4.3 이후부터는 생성자가 하나라면 `@Autowired`를 생략할 수 있듯, `@Value`도 생성자 파라미터에 직접 사용할 수 있음
```java
@Component
public class MyService {
    private final String apiKey;

    // 불변성 유지 및 테스트 코드 작성(직접 인자 전달)이 용이함
    public MyService(@Value("${api.key}") String apiKey) {
        this.apiKey = apiKey;
    }
}
```

### 3) Setter 주입 (거의 사용 안 함)
```java
@Component
public class MyService {
    private String apiKey;

    @Value("${api.key}")
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
```

---

## 3. @Value vs @ConfigurationProperties

실무에서는 설정값의 성격에 따라 두 어노테이션을 구분하여 사용함

### 1) @Value (단일 값 주입)
*   **용도**: API Key, 파일 경로 등 **한두 개의 독립적인 설정값**을 가져올 때 사용함
*   **특징**: SpEL(Spring Expression Language)을 사용할 수 있어 복잡한 연산이 가능함

### 2) @ConfigurationProperties (그룹화된 설정 주입)
*   **용도**: DB 설정, 메일 서버 설정 등 **계층 구조를 가진 연관된 설정값들**을 하나의 객체로 묶을 때 사용함
*   **장점**:
    *   **유연한 바인딩 (Relaxed Binding)**: `my-api-key`, `my_api_key`, `myApiKey`를 모두 동일하게 매핑해줌
    *   **검증 가능**: `@Validated`와 함께 사용하여 설정값이 누락되었거나 형식이 틀렸을 때 실행 시점에 에러를 낼 수 있음

### 실무 비교 예시
```yaml
# application.yml
storage:
  bucket-name: my-bucket
  region: ap-northeast-2
  max-size: 1024
```

```java
// @ConfigurationProperties 방식 (권장)
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String bucketName;
    private String region;
    private long maxSize;
    // getter, setter...
}

// @Value 방식 (번거롭고 오타 위험)
public class MyService {
    @Value("${storage.bucket-name}") String bucket;
    @Value("${storage.region}") String region;
    @Value("${storage.max-size}") long size;
}
```

---

## 4. 요약 및 선택 가이드

| 구분 | @Value | @ConfigurationProperties |
| :--- | :--- | :--- |
| **바인딩 방식** | 필드 단위 (SpEL 지원) | 클래스/객체 단위 (계층 구조) |
| **유연한 바인딩** | 지원하지 않음 (정확히 일치해야 함) | **지원함** (케밥 케이스 등 허용) |
| **주요 용도** | 간단한 단일 설정값 | 복잡하고 연관된 다수의 설정값 |
| **검증(Validation)** | 불가능 | **가능** (`@Min`, `@NotNull` 등) |

### 결론
> **간단한 설정은 `@Value`를 사용하되, 연관된 설정이 3개 이상이거나 계층 구조를 가진다면 가독성과 검증을 위해 `@ConfigurationProperties`를 사용하는 것이 실무 표준**
