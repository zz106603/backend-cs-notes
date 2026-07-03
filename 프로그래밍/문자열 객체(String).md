### String 객체 (Java)

**정의**
- 자바에서 문자열을 다루는 가장 기본적인 클래스
- **불변(Immutable)** 객체. 한 번 생성된 문자열은 절대 변경되지 않음

---

### 핵심 특징: 불변성 (Immutability)

**왜 불변인가?**
- 내부적으로 문자열 데이터를 저장하는 `byte[]` (Java 9 이전엔 `char[]`) 배열이 `final`로 선언되어 있어 수정이 불가능
- `concat()`, `replace()`, `toUpperCase()` 같은 메서드를 호출하면, 원본을 바꾸는 게 아니라 **새로운 String 객체를 생성해서 반환**

**불변으로 설계한 이유 (장점)**
1.  **String Constant Pool (메모리 절약):** 똑같은 문자열("Hello")을 여러 변수가 공유해서 쓸 수 있음
2.  **Thread-Safe (동기화 필요 없음):** 여러 스레드가 동시에 접근해도 값이 변하지 않으므로 안전함
3.  **보안 (Security):** DB 연결 URL, 파일 경로, 패스워드 등이 중간에 변경될 위험이 없음
4.  **HashCode 캐싱:** 문자열의 해시코드를 한 번 계산하면 캐싱해두고 재사용하므로, `HashMap`의 키로 쓸 때 성능이 좋음

---

### 메모리 구조: 리터럴 vs new String()

**1. 리터럴 방식 (`String s = "Hello";`)**
- **저장 위치:** Heap 영역 내부의 **String Constant Pool (문자열 상수 풀)**
- **특징:** 이미 같은 문자열이 풀에 있다면, 새로 만들지 않고 **기존 객체의 주소를 재사용** (메모리 효율 극대화)

**2. 생성자 방식 (`String s = new String("Hello");`)**
- **저장 위치:** 일반 **Heap 영역**
- **특징:** 풀을 거치지 않고 무조건 **새로운 객체를 생성**. 같은 문자열이라도 주소값이 다름 (메모리 낭비)

**실무 팁:** 절대 `new String("...")` 방식을 쓰지 말고, 항상 리터럴(`"..."`)을 사용

```java
String str1 = "Hello"; 
String str2 = "Hello"; 
String str3 = new String("Hello");

System.out.println(str1 == str2); // true (같은 주소 공유)
System.out.println(str1 == str3); // false (다른 주소)
System.out.println(str1.equals(str3)); // true (내용은 같음)
```

---

### 실무에서의 성능 이슈 (StringBuilder 사용)

**문제점 (String + 연산)**
- String은 불변이므로, `+` 연산자로 문자열을 합칠 때마다 **계속 새로운 객체가 생성**됨
- 반복문(Loop) 안에서 `+` 연산을 하면 메모리 낭비(GC 부하)가 심각해짐

```java
// [BAD] 매번 새로운 객체 생성 -> 버려짐 -> GC 발생
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i; 
}
```

**해결책 (StringBuilder / StringBuffer)**
- **가변(Mutable)** 객체. 내부 버퍼(배열)를 직접 수정하므로 새로운 객체를 만들지 않음
- **StringBuilder:** 동기화 지원 X (빠름, 단일 스레드용) -> **실무에서 대부분 이거 씀**
- **StringBuffer:** 동기화 지원 O (느림, 멀티 스레드용)

```java
// [GOOD] 하나의 객체만 사용
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

---

### Q&A: intern() 메서드는 뭔가요?

- `new String("Hello")`로 힙에 만들어진 문자열을 강제로 **String Constant Pool**로 이동(또는 조회)시키는 메서드
- **실무 사용:** 거의 안 씀. `intern()`을 너무 많이 호출하면 상수 풀이 꽉 차서 성능 저하가 발생할 수 있음. 그냥 리터럴을 쓰면 알아서 풀에 들어감