---
title: "Java 핵심 정리"
tags:
  - "Java"
  - "JVM"
---
# Java 핵심 정리

## Java와 JVM 기본 구조

Java는 작성한 소스 코드를 바로 운영체제에서 실행하지 않고, 컴파일한 뒤 JVM을 통해 실행

### JDK와 JVM
**JDK(Java Development Kit)** 는 Java 프로그램을 개발하고 실행하기 위한 도구 모음
- Java 컴파일러
- JVM
- Java 기본 라이브러리
- 각종 개발 도구

**JVM(Java Vertual Machine)** 은 컴파일된 Java 코드를 실제로 실행하는 환경
```
Java Source Code
      ↓
   Compile
      ↓
   Bytecode
      ↓
     JVM
      ↓
Operating System
```
Java 프로그램은 JVM 위에서 실행되기 때문에 운영체제가 달라져도 해당 운영체제에 맞는 JVM이 있다면 같은 Java 프로그램을 실행할 수 있음

### Java 코드 실행 과정
```
Main.java
   ↓
 javac
   ↓
Main.class
```
Java 컴파일러인 `javac`가 소스 코드를 컴파일

`.class` 파일에는 JVM이 실행할 수 있는 **Bytecode**가 들어있음
```
.java
 ↓ 컴파일
.class
 ↓
JVM
 ↓
실행
```

## JVM 메모리 구조
Java 프로그램이 실행되면 JVM은 용도에 따라 메모리를 나누어 사용

### Stack
**Thread마다 독립적으로 존재하는 메모리 영역**

Method가 호출될 때 실행에 필요한 정보가 Stack에 만들어짐
- 지역 변수
- 매개 변수
- 객체를 가리키는 참조값
- Method 실행 정보

### Heap
Java에서 생성한 **객체와 배열 등이 저장되는 공간**
```
Stack      Heap
user ─→ User 객체
```
**Heap은 여러 Thread가 공유**
- 여러 Thread가 같은 객체의 상태를 동시에 변경하면 동시성 문제 발생 가능

### Metaspace
Class와 관련된 정보가 저장
- Class 정보
- Method 정보
- Field 정보

### 기본형과 참조형
**기본형** - **값 자체**를 저장
```Java
int age = 30;
boolean active = true;
```
- int
- long
- double
- boolean
- char

**참조형** - 객체 자체가 아니라 **객체를 가리키는 참고값**을 가짐
```Java
User user = new User();
```
```
user
 ↓
참조값
 ↓
User 객체
```
`String`, 배열, 직접 만든 Class 등은 참조형

### Java는 항상 값으로 전달
Java는 Method에 값을 전달할 때 항상 **값을 복사해서 전달**

기본형
```Java
int age = 30;
change(age);
// 30이라는 값이 복사되어 전달
```

참조형
```Java
User user = new User();
change(user);
```
- 객체 자체를 전달하는 것이 아니라 **객체를 가리키는 참조값을 복사해서 전달**
- 따라서 Method 내부에서 같은 객체를 수정할 수 있지만 Java 자체는 항상 값 전달 방식

## 객체 비교와 불변성

### `==`와 `equals()`
참조형에서 `==`는 두 변수가 **같은 객체를 가리키는지** 확인
```Java
User a = new User("Kim");
User b = new User("Kim");
a == b;
```
두 객체의 내용이 같더라도 서로 다른 객체이기 때문에 일반적으로 `false`

반면 `equals()`는 객체의 **논리적인 값이 같은지** 판단할 때 사용
```Java
a.equals(b);
```
직접 만든 Class에서는 어떤 기준으로 같은 객체라고 판단할 것인지 `equals()`를 구현할 수 있음

### `equals()`와 `hashCode()`
equals()를 재정의한다면 일반적으로 hashCode()도 함께 재정의 해야 함

- HashMap
- HashSet
같은 자료구조에서 중요함

**Hash 기반 자료구는 객체를 찾을 때 `hashCode()`로 먼저 위치를 찾고, 필요하면 `equals()`로 실제 같은 객체인지 비교**

### String
Java에서 자주 사용되는 객체이며 중요한 특징 중 하나는 **불변 객체**라는 것
```Java
String name = "Kim";
name = name + " Lee";
```
기존 문자열 자체가 변경되는 것이 아니라 새로운 문자열이 만들어짐

**String이 불변이면:**
- 여러 곳에서 안전하게 공유하기 쉬움
- Hash 값이 변경되지 않음
- 여러 Thread가 사용할 때도 상태 변경 문제가 줄어듬

### String Pool
문자열 Literal은 JVM에서 같은 문자열 객체를 재사용할 수 있음
```Java
String a = "hello";
String b = "hello";
```
개념적으로 `a -> "hello" <- b`처럼 같은 문자열을 가리킬 수 있음

반면 아래는 별도의 객체가 만들어질 수 있음
```Java
String c = new String("hello");
```

따라서 String 값을 비교할 때는 `==`가 아니라 `equals()`를 사용해야 함

### StringBuilder
String은 불변이기 때문에 반복해서 문자열을 이어붙이면 새로운 객체가 계속 만들어질 수 있음
```Java
String result = "";
for (...) {
    result += value;
}
```
반복적인 문자열 변경이 많다면 `StringBuilder`를 사용할 수 있음
```Java
StringBuilder sb = new StringBuilder();
sb.append("A");
sb.append("B");
```

### `final`과 불변 객체의 차이
```Java
final List<String> list = new ArrayList<>();
```
`final`이라고 해서 List 내부 데이터를 변경할 수 없는 것은 아님
```Java
list.add("A"); // 가능
```

하지만 다른 List를 다시 대입할 수는 없음
```Java
list = new ArrayList<>() // 불가능
```

## Collection Framework

### List
- 순서가 있고 중복을 허용
- 대표적인 구현체는 `ArrayList`

```Java
List<String> users = new ArrayList<>();
users.add("Kim");
users.add("Lee");
users.add("Kim");

// Kim, Lee, Kim
```

### Set
- 중복을 허용하지 않는 데이터 집합에 사용
- 대표적인 구현체는 `HashSet`
```Java
Set<String> users = new HashSet<>();
users.add("Kim");
users.add("Kim");

// Kim
```

### Map
- Key와 Value 형태로 데이터를 저장
- Key는 중복될 수 없음
```Java
Map<Long, User> users = new HashMap<>();
users.put(1L, user);

// 1 -> User A
```

### ArrayList
- ArrayList는 내부적으로 배열 기반으로 데이터를 관리
- `[A][B][C][D]`
- Index를 이용한 조회는 빠름 -> 대략 `O(1)`에 접근 가능
- 하지만 중간 데이터를 삭제하거나 삽입하면 뒤의 데이터들을 이동해야 할 수 있음 -> 상대적으로 비용 발생

### HashMap
- HashMap은 Key를 이용해 데이터를 빠르게 찾는 구조
- Key의 `hashCode()`를 이용해 저장 위치를 찾음
- 조회할 때도 같은 방식으로 접근
- 평균적으로 빠른 조회가 가능한 이유

### Hash 충돌
- 서로 다른 Key가 같은 위치를 가리키는 경우가 발생할 수 있음
- `Key A -> Bucket <- Key B`
- 이 경우 HashMap은 같은 위치에 있는 여러 값 중 실제 Key를 찾기 위해 `equals()`등을 사용

### List / Set / Map 선택 기준
```
순서가 있는 여러 데이터
→ List

중복을 제거해야 함
→ Set

Key를 이용해 데이터를 찾음
→ Map
```
```
사용자 목록
→ List<User>

권한 종류
→ Set<String>

userId로 사용자 찾기
→ Map<Long, User>
```

### Generic
- Collection 등에 저장할 데이터 타입을 미리 정할 수 있도록 함
```Java
List<String> names = new ArrayList<>();
```
- 이 List에는 String만 넣을 수 있음
- 잘못된 타입을 넣는 문제를 컴파일 시점에 확인할 수 있기 때문에 **타입 안정성**을 높여줌

## 예외 처리
Java에서는 프로그램 실행 중 문제가 발생했을 때 Exception을 사용
```Java
try {
    // 실행
} catch (Exception e) {
    // 예외 처리
}
```

### Checked Exception
- 컴파일러가 예외 처리를 요구
- 대표적으로 `IOException` 등
- 직접 처리하거나 호출한 쪽으로 넘겨야 함
```Java
try {} catch (IOException e) {}

throws IOException
```

### Unchecked Exception
- 컴파일러가 처리를 강제하지 않는 예외
- `RuntimeException` 계열이 대표적
  - `NullPointerException`
  - `IllegalArgumentException`
- Spring의 비즈니스 예외도 RuntimeException을 상속해서 만드는 경우가 많음
```Java
public class UserNotFoundException
        extends RuntimeException {
}
```

### Error와 Exception
Java의 예외 구조를 단순화하면 다음과 같음
```
Throwable
 ├─ Error
 └─ Exception
```
- `Exception`은 Application에서 처리할 수 있는 문제를 표현하는 데 사용
- `Error`는 JVM이나 시스템 수준의 심각한 문제를 나타내는 경우가 많음
  - `OutOfMemoryError`
  - `StackOverflowError`
  - 일반적인 비즈니스 로직에서는 Error를 잡아서 정상 흐름으로 처리하려고 하지 않음

## Garbage Collection과 메모리 문제

### Garbage Collection
Java에서는 개발자가 일반적으로 객체의 메모리를 직접 해제하지 않음

JVM의 **GC(Garbage Collector)** 가 더 이상 사용되지 않는 객체를 찾아 정리
```
Heap

Object A ← 사용 중
Object B ← 사용 중
Object C ← 더 이상 접근할 수 없음
               ↓
               GC
```

### GC는 어떤 객체를 제거하는가
단순히 오래된 객체를 제거하는 것이 아니라, 특정 기준점에서 객체에 도달할 수 있는지를 확인함

이 기준점을 **GC Root**라고 함
```
GC Root
   ↓
Object A
   ↓
Object B
```
A와 B는 아직 접근할 수 있으므로 살아 있는 객체

반면
```
Object C → Object D
```
가 존재하더라고 GC Root에서 C까지 도달할 방법이 없다면 C와 D는 GC 대상이 될 수 있음

> **더 이상 접근할 수 없는 객체를 GC가 정리**

### Memory Leak
Java에서 Memory Leak이 발생할 수 있음
```Java
private final List<User> users = new ArrayList<>();
```
여기에 계속 객체를 추가하고 제거하지 않는다고 가정
```
users
 ↓
User
User
User
User
...
```
List가 계속 객체들을 참조하고 있기 때문에 GC가 제거할 수 없음

결국 Heap 사용량이 계속 증가

### OutOfMemoryError
JVM이 필요한 메모리를 더 이상 확보할 수 없을 때 발생할 수 있음
```
객체 계속 생성
   ↓
Heap 부족
   ↓
OutOfMemoryError
```
예를 들어 DB에서 지나치게 많은 데이터를 한번에 조회해 Java List에 담는 경우도 원인이 될 수 있음
```
Database
   ↓
수백만 Row
   ↓
Java List
   ↓
Heap 부족
```

### StackOverflowError
Method 호출이 지나치게 깊어져 Thread의 Stack을 모두 사용하면 발생

대표적으로 무한 재귀
```
public void test() {
    test();
}
```
```
test()
 ↓
test()
 ↓
test()
 ↓
...
 ↓
StackOverflowError
```

## Java 동시성
Java 동시성은 Thread 개념을 Java에 연결해서 이해하면 됨

일반적인 Java Thread는 OS Thread와 연결되어 실행됨
```
Java Thread
     ↓
OS Thread
     ↓
    CPU
```

### Thread Safety
여러 Thread가 동시에 사용해도 문제가 발생하지 않는 코드를 **Thread Safe**하다고 함
```Java
public class Counter {
    private int count;
    public void increase() {
        count++;
    }
}
```
여러 Thread가 동시에 실행하면 **Race Condition**이 발생할 수 있음

### synchronized
여러 Thread가 특정 코드 영역을 동시에 실행하지 못하도록 제어
```Java
public synchronized void increase() {
    count++;
}
```
```
Thread A → 실행
Thread B → 대기
Thread C → 대기
```
공유 데이터를 보호할 수 있지만 Thread 대기가 증가할 수 있으므로 필요한 부분에 사용해야 함

### volatile
한 Thread가 변경한 공유 변수의 값을 다른 Thread가 제대로 볼 수 있도록 하는 데 사용
```Java
private volatile boolean running = true;
```
하지만 다음 코드는 여전히 안전하지 않음
```Java
volatile int count;
count++;
```

### static과 공유 데이터
`static` 변수는 객체마다 존재하는 것이 아니라 Class 기준으로 공유됨
```Java
public class User {
    static int count;
}
```
```
User A ─┐
User B ─┼→ User.count
User C ─┘
```
따라서 변경 가능한 `static` 변수도 여러 Thread가 동시에 접근한다면 동시성 문제가 발생할 수 있음

## Spring과 연결해서 이해
Spring Bean은 기본적으로 Singleton
```Java
@Service
public class OrderService { }
```
즉 Application에서 일반적으로 하나의 `OrderService` 객체가 생성되고 여러 요청이 이를 사용함
```
Request A → Thread A ─┐
Request B → Thread B   → OrderService
Request C → Thread C ─┘
```
따라서 다음처럼 변경 가능한 상태를 Bean 내부에 저장하면 위험함
```Java
@Service
public class OrderService {
    private int count;
}
```
여러 Thread가 같은 `count`를 동시에 변경할 수 있기 때문임

그래서 일반적인 Spring Service는 요청별 상태를 Instance Field에 보관하지 않는 **Stateless한 구조**로 만드는 것이 중요함

## 백엔드 개발자 답변

### JDK와 JVM의 차이는?
- JDK는 Java 프로그램을 개발하고 실행하기 위한 전체 도구
- JVM은 컴파일된 Java Bytecode를 실제로 실행하는 환경

### Java 코드는 어떻게 실행되는가?
- `.java` 파일을 컴파일 하면 `.class` Bytecode가 만들어지고 JVM이 이를 실행

### Stack과 Heap의 차이는?
- Stack은 Thread마다 독립적으로 존재하며 Method 실행 정보와 지역 변수 등을 관리
- Heap에는 객체가 저장되며 여러 Thread가 공유하고 GC의 관리 대상이 됨

### Java는 객체를 참조 전달하는가?
- Java는 항상 값으로 전달
- 객체를 전달할 때도 객체 자체가 아니라 객체가 가리키는 참조값이 복사되어 전달됨

### `==`와 `equals()`의 차이는?
- 참조형에서 `==`은 같은 객체를 가리키는지 비교
- `equals()`는 논리적으로 같은 값인지 비교하는 데 사용

### `equals()`와 `hashCode()`를 함께 재정의하는 이유?
- `HashMap`, `HashSet` 같은 Hash 기반 Collection이 객체를 찾을 때 두 Method를 함께 사용

### String은 왜 불변인가?
- 생성된 String의 값을 변경할 수 없도록 해 안전하게 공유할 수 있음
- Hash 값 유지나 String Pool 사용에도 유리

### HashMap은 어떻게 데이터를 찾는가?
- Key의 `hashCode()`를 이용해 저장 위치를 찾고
- 충돌이 발생한 경우 `equals()` 등을 이용해 실제 Key를 구분

### Checked Exception과 Unchecked Exception의 차이는?
- Checked Exception은 컴파일러가 처리나 전달을 강제
- Unchekced Exception은 `RuntimeException` 계열로 컴파일러가 처리를 강제하지 않음

### GC는 어떤 객체를 제거하는가?
- GC Root에 더 이상 도달할 수 없는 객체를 정리

### Java에서도 Memory Leak이 발생할 수 있은가?
- 가능
- 사용하지 않는 객체라도 다른 객체가 계속 참조하고 있다면 GC가 제거할 수 없음

### `synchronized`와 `volatile`의 차이는?
- synchronized는 여러 Thread의 동시 실행을 제어
- volatile은 공유 변수의 변경된 값을 다른 Thread가 볼 수 있도록 하는 데 사용

## 핵심 흐름
```
1. Java 실행

.java
 ↓
컴파일
 ↓
.class
 ↓
JVM
 ↓
실행
```
```
2. Memory

Stack
→ Thread별 실행 정보

Heap
→ 객체
→ 여러 Thread 공유
→ GC 관리

Metaspace
→ Class 정보
```
```
3. 객체와 Collection

객체
 ↓
== / equals()
 ↓
hashCode()
 ↓
HashMap / HashSet
```
```
4. 동시성

여러 Thread
 ↓
같은 Heap 객체 공유
 ↓
Race Condition
 ↓
synchronized / volatile
 ↓
Thread Safety
```
