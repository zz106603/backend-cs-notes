### 자바 Object -> String 변환 방법 비교

자바에서 `Object` 타입의 값을 `String`으로 변환할 때 사용하는 대표적인 3가지 방법(`(String)`, `String.valueOf()`, `toString()`)의 차이점과 주의사항을 정리

---

### 1. (String) 캐스팅 (Type Casting)

**특징**
- 객체의 타입을 강제로 `String`으로 변환함
- **대상 객체가 `String` 타입이거나 `null`일 때만** 정상 동작함

**주의사항**
- **ClassCastException:** 만약 객체가 `String`이 아닌 다른 타입(예: `Integer`)이라면 런타임 에러가 발생
- **Null:** 대상이 `null`이면 에러 없이 `null`을 반환

```java
Object obj = "Hello";
String str = (String) obj; // "Hello" (성공)

Object nullObj = null;
String str2 = (String) nullObj; // null (성공)

Object intObj = 123;
String str3 = (String) intObj; // ClassCastException 발생! (실패)
```

---

### 2. String.valueOf()

**특징**
- `Object`의 값을 문자열로 변환하는 **가장 안전한 방법**
- 내부적으로 `obj == null ? "null" : obj.toString()` 처럼 동작함
- **어떤 타입의 객체든** 에러 없이 문자열로 변환해줍니다.

**주의사항**
- **"null" 문자열:** 대상이 `null`이면 실제 `null`이 아니라 **문자열 "null"** 을 반환
- 이 때문에 `if (str.equals("null"))` 같은 체크가 필요할 수 있어 주의해야 함

```java
Object obj = 123;
String str = String.valueOf(obj); // "123" (성공)

Object nullObj = null;
String str2 = String.valueOf(nullObj); // "null" (문자열 "null" 반환!)
// str2 == null -> false
// str2.equals("null") -> true
```

---

### 3. toString()

**특징**
- `Object` 클래스의 메서드를 직접 호출함
- 가장 직관적이지만, **Null 안전성이 없음**

**주의사항**
- **NullPointerException:** 대상이 `null`이면 메서드를 호출할 수 없으므로 즉시 NPE가 발생
- 따라서 반드시 `null` 체크를 먼저 해야 함

```java
Object obj = 123;
String str = obj.toString(); // "123" (성공)

Object nullObj = null;
String str2 = nullObj.toString(); // NullPointerException 발생! (실패)
```

---

### 요약: 언제 무엇을 써야 할까?

| 방법 | Null일 때 | 다른 타입일 때 | 추천 상황 |
| :--- | :--- | :--- | :--- |
| **(String) obj** | `null` 반환 | **ClassCastException** | 객체가 확실히 String 타입일 때만 사용 |
| **String.valueOf(obj)** | `"null"` (문자열) | `toString()` 결과 | **가장 안전함.** 어떤 값이 들어올지 모를 때 (로깅 등) |
| **obj.toString()** | **NullPointerException** | `toString()` 결과 | 객체가 절대 null이 아님이 보장될 때 |

**실무 팁:**
- **데이터가 확실하지 않다면 `String.valueOf()`를 쓰는 것이 좋음** (단, "null" 문자열 주의)
- **Null 처리가 중요하다면** `Objects.toString(obj, defaultVal)`을 사용하는 것도 좋은 방법
    - 예: `Objects.toString(obj, "")` -> null이면 빈 문자열 반환.
  