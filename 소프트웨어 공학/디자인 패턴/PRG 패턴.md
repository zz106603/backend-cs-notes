# PRG 패턴 (Post-Redirect-Get Pattern)

웹 애플리케이션 개발에서 폼(Form) 중복 제출을 방지하기 위해 사용하는 필수적인 디자인 패턴

---

## 1. 왜 필요한가요? (문제 상황)

### 상황: 쇼핑몰 상품 주문
1.  사용자가 상품 주문 폼을 작성하고 **[결제하기]** 버튼을 클릭 (POST 요청)
2.  서버는 주문 정보를 DB에 저장하고, "주문 완료" 페이지(HTML)를 그대로 응답으로 내려줍니다. (200 OK)
3.  이 상태에서 사용자가 실수로 **[새로고침]** 버튼을 누름

### 문제 발생: 중복 주문
*   브라우저는 **"마지막으로 보낸 요청을 다시 보낼까요?"** 라고 물음 (양식 다시 제출 경고창)
*   사용자가 "확인"을 누르면, 아까 보냈던 **POST 요청(주문 생성)이 서버로 또 전송**됨
*   결과적으로 **주문이 2건 생성되고, 결제도 2번 중복**으로 일어날 수 있음

---

## 2. PRG 패턴의 동작 원리

이 문제를 해결하기 위해 **POST 요청 처리 후, 결과를 보여주는 페이지로 리다이렉트(Redirect)** 시키는 방식을 사용

### 단계별 흐름
1.  **POST (요청)**: 사용자가 폼을 작성하고 제출
    *   `POST /orders`
2.  **Redirect (응답)**: 서버는 주문을 처리한 후, 뷰(HTML)를 바로 주지 않고 **"저쪽 주소로 가세요"** 라는 응답을 보냄
    *   HTTP Status: `302 Found` 또는 `303 See Other`
    *   Header: `Location: /orders/100` (주문 상세 페이지 URL)
3.  **GET (재요청)**: 브라우저는 서버가 알려준 주소로 자동으로 이동
    *   `GET /orders/100`
4.  **응답**: 서버는 주문 상세 페이지(HTML)를 보여줌

### 결과: 새로고침을 해도 안전함
*   이제 사용자가 [새로고침]을 누르면, 마지막 요청인 **3번 단계(GET /orders/100)** 가 다시 실행됨
*   단순히 주문 내역을 **조회(GET)** 하는 요청이므로, 몇 번을 새로고침해도 주문이 중복으로 들어가지 않음 (멱등성 보장)

---

## 3. 실무 코드 예시 (Spring MVC)

### 안 좋은 예 (Forward 방식)
```java
@PostMapping("/orders")
public String addOrder(Order order) {
    orderRepository.save(order);
    // 뷰 템플릿을 그대로 반환 (새로고침 시 중복 주문 위험)
    return "order/orderComplete"; 
}
```

### 좋은 예 (PRG 패턴 적용)
```java
@PostMapping("/orders")
public String addOrder(Order order) {
    Order savedOrder = orderRepository.save(order);
    // 저장 후 상세 페이지로 리다이렉트 (URL 변경됨)
    return "redirect:/orders/" + savedOrder.getId();
}

@GetMapping("/orders/{id}")
public String getOrder(@PathVariable Long id, Model model) {
    Order order = orderRepository.findById(id);
    model.addAttribute("order", order);
    return "order/orderDetail";
}
```

---

## 4. 요약

*   **Post**: 데이터 변경 요청 (저장, 수정, 삭제)
*   **Redirect**: 작업이 끝나면 결과 화면으로 이동 명령
*   **Get**: 결과 화면 조회

이 패턴을 사용하면 **사용자 경험(UX)** 이 좋아지고(경고창 안 뜸), **서버 데이터 정합성**도 지킬 수 있음
