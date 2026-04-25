# 함수 호출 (Function Calling / Tool Use)

LLM이 사용자의 질문에 답하기 위해 **외부 도구(API, 데이터베이스, 로컬 함수 등)를 호출해야 할 필요가 있다고 스스로 판단하고, 그에 필요한 인자(Arguments)를 구조화된 형태(JSON)로 출력**하는 기술입니다.

---

## 1. 왜 필요한가요? (LLM의 한계 극복)

LLM은 똑똑하지만 다음과 같은 근본적인 한계가 있습니다.

1.  **실시간 데이터 부재**: "오늘 서울 날씨 알려줘"라고 물으면 학습 시점 이후의 정보는 알 수 없습니다.
2.  **수치 연산 취약**: 복잡한 계산기 연산이나 데이터베이스 쿼리 결과 등을 정확히 처리하지 못할 때가 많습니다.
3.  **외부 액션 불가능**: "내 일정에 회의 추가해줘"라고 해도 실제 캘린더에 접근할 권한이나 수단이 없습니다.

**Function Calling은 LLM에게 "손과 발"을 달아주어 외부 시스템과 상호작용하게 합니다.**

---

## 2. 동작 원리 (Process)

1.  **정의 (Define)**: 개발자가 LLM에게 사용할 수 있는 함수의 이름, 설명, 파라미터 구조를 전달합니다.
2.  **판단 (Decide)**: 사용자의 질문이 들어오면 LLM은 "내가 직접 답할 수 있는가?" 아니면 "제공된 함수를 써야 하는가?"를 판단합니다.
3.  **출력 (Output)**: 함수를 써야 한다고 판단하면, 함수 이름과 파라미터값이 담긴 **JSON 객체**를 반환합니다. (함수를 직접 실행하는 것이 아님!)
4.  **실행 (Execute)**: 백엔드 서버가 이 JSON을 받아 **실제 함수를 실행**하고 결과를 얻습니다.
5.  **답변 (Respond)**: 함수의 결과값을 다시 LLM에게 전달하면, LLM이 이를 바탕으로 최종 답변을 구성합니다.

---

## 3. 실무 코드 예시 (OpenAI API 기준)

### 1) 함수 정의 (Tools definition)
LLM에게 어떤 도구를 쓸 수 있는지 알려주는 명세서입니다.

```json
{
  "type": "function",
  "function": {
    "name": "get_order_status",
    "description": "주문 번호를 입력받아 현재 배송 상태를 조회합니다.",
    "parameters": {
      "type": "object",
      "properties": {
        "order_id": {
          "type": "string",
          "description": "주문 번호 (예: ORD-123)"
        }
      },
      "required": ["order_id"]
    }
  }
}
```

### 2) 백엔드 로직 흐름
사용자가 "내 주문 ORD-1004 어디쯤 왔어?"라고 물었을 때의 처리 과정입니다.

```python
# 1. 사용자의 질문을 LLM에게 도구 명세와 함께 전달
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "ORD-1004 배송 상태 알려줘"}],
    tools=my_tools # 위에서 정의한 함수 명세
)

# 2. LLM이 함수 호출이 필요하다고 판단한 경우 (tool_calls 존재)
tool_call = response.choices[0].message.tool_calls[0]
if tool_call:
    function_name = tool_call.function.name
    arguments = json.loads(tool_call.function.arguments)
    
    # 3. 실제 DB 조회 수행 (백엔드 코드)
    if function_name == "get_order_status":
        db_result = db.query(f"SELECT status FROM orders WHERE id = '{arguments['order_id']}'")
        
    # 4. DB 결과값을 다시 LLM에게 전달하여 최종 응답 생성
    final_response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "user", "content": "ORD-1004 배송 상태 알려줘"},
            response.choices[0].message, # 모델의 함수 호출 요청 메시지
            {"role": "tool", "content": db_result, "tool_call_id": tool_call.id} # 도구 실행 결과
        ]
    )
```

---

## 4. 실무 주의사항 및 팁

1.  **보안 (Prompt Injection)**: LLM이 생성한 인자값을 그대로 DB 쿼리에 넣으면 위험합니다. (예: `order_id`에 `' OR 1=1 --` 주입). 반드시 백엔드에서 **파라미터 유효성 검사**를 거쳐야 합니다.

2.  **함수 설명(Description)의 중요성**: LLM은 함수의 설명을 보고 호출 여부를 결정합니다. "조회 함수"보다는 "특정 주문 번호에 대한 현재 배송 상태(배송중, 완료 등)를 조회하는 함수"처럼 구체적으로 적어야 정확도가 올라갑니다.
3.  **토큰 비용**: 함수 명세(Tools)도 모두 입력 토큰에 포함됩니다. 너무 많은 함수를 한꺼번에 넣으면 비용이 올라가고 모델이 혼란을 느낄 수 있으므로 필요한 도구만 선별해서 넣어야 합니다.

---

### 요약

| 구분 | 일반 텍스트 생성 | 함수 호출 (Function Calling) |
| :--- | :--- | :--- |
| **핵심 역할** | 질문에 대한 지식 기반 답변 | **외부 시스템 액션 및 데이터 확보** |
| **출력 형태** | 자유로운 텍스트 | **구조화된 JSON (이름, 인자)** |
| **백엔드 역할** | 결과 전달 | **JSON 해석 및 실제 로직 실행** |
| **주요 활용** | 요약, 번역, 일반 상담 | **DB 조회, 메일 발송, API 연동** |

> **백엔드 개발자에게 Function Calling은 LLM을 "똑똑한 챗봇"에서 "강력한 업무 자동화 에이전트"로 진화시키는 핵심 도구입니다.**
