### 자바 제네릭: 공변, 반공변, 무공변

**핵심 개념**
- **제네릭은 기본적으로 무공변(Invariant)**
- 즉, `S`가 `T`의 자식 타입이라도, `List<S>`와 `List<T>`는 아무 관계가 없음 (상속 관계가 유지되지 않음)

**비유: "과일 바구니"**
- `사과`는 `과일` (상속 관계 O)
- 하지만 `사과 바구니`는 `과일 바구니`가 아님 (상속 관계 X)
    - 만약 `사과 바구니`를 `과일 바구니`로 취급한다면, 누군가 거기에 `바나나`를 넣을 수도 있기 때문임 (타입 안정성 깨짐)

---

### 1. 무공변 (Invariant) - 기본값

- **정의:** 타입 `S`와 `T`가 상속 관계여도, `List<S>`와 `List<T>`는 전혀 다른 타입으로 취급함
- **특징:** 읽기/쓰기 모두 안전하지만, 유연성이 떨어짐

```java
class Fruit {}
class Apple extends Fruit {}

// [무공변] 컴파일 에러 발생
List<Fruit> fruits = new ArrayList<Apple>(); 
// "사과 리스트는 과일 리스트가 아니다."
```

---

### 2. 공변 (Covariant) - `<? extends T>`

- **정의:** `S`가 `T`의 자식이라면, `List<S>`는 `List<? extends T>`의 자식으로 인정해줌
- **특징:** **읽기(Read) 전용** (꺼낼 수는 있지만, 넣을 수는 없음)
- **비유:** "이 바구니에는 `Fruit`의 자식들이 들어있음" -> 꺼내면 무조건 `Fruit`임이 보장됨

```java
// [공변] OK
List<? extends Fruit> fruits = new ArrayList<Apple>();

// 1. 읽기 (가능): 꺼내면 최소한 Fruit임은 확실함.
Fruit fruit = fruits.get(0); 

// 2. 쓰기 (불가능): 컴파일 에러
// fruits가 Apple 리스트인지, Banana 리스트인지 모르므로 아무것도 넣을 수 없음.
fruits.add(new Apple()); // Error
fruits.add(new Fruit()); // Error
```

---

### 3. 반공변 (Contravariant) - `<? super T>`

- **정의:** `S`가 `T`의 부모라면, `List<S>`는 `List<? super T>`의 자식으로 인정해줌 (상속 관계가 뒤집힘)
- **특징:** **쓰기(Write) 전용** (넣을 수는 있지만, 꺼낼 때는 `Object`로만 꺼내짐)
- **비유:** "이 바구니는 `Apple`의 부모들을 담을 수 있음" -> `Apple`을 넣어도 안전함

```java
// [반공변] OK (Apple의 부모인 Fruit, Object 리스트 가능)
List<? super Apple> apples = new ArrayList<Fruit>();

// 1. 쓰기 (가능): Apple과 그 자식들은 안전하게 넣을 수 있음.
apples.add(new Apple());
apples.add(new GreenApple());

// 2. 읽기 (제한적): 꺼내면 뭐가 나올지 모름 (Fruit일 수도, Object일 수도)
// 따라서 Object로만 받아야 함.
Object obj = apples.get(0);
```

---

### PECS 공식 (Producer-Extends, Consumer-Super)

이펙티브 자바(Effective Java)에서 제시한, 와일드카드를 언제 써야 할지 결정하는 공식

1.  **Producer (생산자) -> `extends`**
    - 데이터를 **제공(꺼내옴)** 하는 객체라면 `<? extends T>`를 씀
    - 예: `List`에서 데이터를 읽어서 다른 곳에 복사할 때

2.  **Consumer (소비자) -> `super`**
    - 데이터를 **소비(저장)** 하는 객체라면 `<? super T>`를 씀
    - 예: `List`에 데이터를 채워 넣을 때

**실무 예시: `Collections.copy(dest, src)`**

```java
// src(원본)에서 데이터를 꺼내서(Producer), dest(목적지)에 넣는다(Consumer).
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (int i = 0; i < src.size(); i++)
        dest.set(i, src.get(i));
}
```
- `src`: 데이터를 제공하므로 **Producer (`extends`)** -> 읽기 전용
- `dest`: 데이터를 받아들이므로 **Consumer (`super`)** -> 쓰기 전용

---

### 요약

| 구분 | 키워드 | 역할 | 읽기 | 쓰기 | 비유 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **무공변** | `T` | 기본 | O | O | **내 바구니** (엄격함) |
| **공변** | `? extends T` | **Producer** | **O** | X | **과일 바구니** (꺼내 먹기용) |
| **반공변** | `? super T` | **Consumer** | X (Object) | **O** | **쓰레기통** (버리기용) |