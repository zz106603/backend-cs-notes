## HTTP 버전 (HTTP/1.1 vs HTTP/2)

HTTP 버전은 클라이언트와 서버가 데이터를 어떤 방식으로 주고받는지를 결정하는 핵심 규약

백엔드 개발에서는 단순히 API 호출이 성공하는 것을 넘어, 버전 차이로 인한 성능, 호환성, 디버깅 문제에 직면할 수 있음

---

### HTTP/1.1: 전통적인 웹 통신 방식

가장 오랫동안 널리 사용된 HTTP 프로토콜 버전

### 1. 특징
- **요청-응답 구조**: 클라이언트의 요청 하나에 서버의 응답 하나가 매칭됨
- **순차 처리**: 하나의 TCP 연결에서 요청과 응답이 순차적으로 처리됨
- **텍스트 기반**: 사람이 읽기 쉬운 텍스트 형식의 헤더와 본문을 사용함
- **Keep-Alive**: 한 번 맺은 TCP 연결을 여러 요청-응답에 재사용하여 연결 생성 비용을 줄임

### 2. 동작 과정
1. **TCP 연결 생성**: 클라이언트와 서버 간 3-way handshake를 통해 TCP 연결을 맺음
2. **HTTP 요청 전송**: 클라이언트가 HTTP 요청 메시지를 보냄
    ```kotlin
    POST /api/users HTTP/1.1
    Host: localhost:8080
    Content-Type: application/json
    Content-Length: 20
    
    {"name":"yun"}
    ```
3. **HTTP 응답 반환**: 서버가 HTTP 응답 메시지를 보냄
    ```kotlin
    HTTP/1.1 200 OK
    Content-Type: application/json
    
    {"result":"ok"}
    ```
4. **연결 유지/종료**: `Connection: keep-alive` 헤더가 있으면 연결을 유지하고 다음 요청에 재사용함

### 3) 문제점: Head-Of-Line Blocking (HOLB)
- **순차 처리의 한계**: 브라우저가 CSS, JS, 이미지, API 등 여러 리소스를 동시에 요청해야 할 때, 각 요청은 이전 요청의 응답이 완료될 때까지 대기해야 함
- **HOLB 발생**: 앞선 요청 중 하나라도 지연되면, 뒤따르는 모든 요청들이 함께 지연되는 현상. 이는 웹 페이지 로딩 속도 저하의 주요 원인이 됨

---

### 2. HTTP/2: 성능 개선을 위한 진화

HTTP/1.1의 HOLB 문제를 해결하고 웹 성능을 향상시키기 위해 등장

"하나의 TCP 연결로 여러 요청을 동시에 처리하자"는 목표를 가짐

### 1) 특징

- **멀티플렉싱 (Multiplexing)**: 가장 중요한 특징. 하나의 TCP 연결 위에서 여러 요청과 응답을 동시에 주고받을 수 있음. HOLB 문제를 해결함
- **바이너리 프레이밍 (Binary Framing)**: 텍스트 기반이 아닌 바이너리 형식으로 데이터를 주고받아 파싱 효율을 높이고 오류 발생 가능성을 줄임
- **헤더 압축 (HPACK)**: 중복되는 헤더 필드를 압축하여 네트워크 오버헤드를 줄임 (예: Authorization, Cookie 등 매 요청마다 반복되는 헤더)
- **스트림 (Stream)**: 각 요청/응답은 고유한 스트림 ID를 가지며, 이 스트림들을 하나의 TCP 연결 안에서 독립적으로 관리함

### 2) 동작 구조

- 클라이언트와 서버는 단 하나의 TCP 연결을 맺음
- 이 연결 안에서 여러 개의 스트림(요청/응답)이 동시에 생성되고 처리됨
    - Stream 1: `GET /users` API 요청
    - Stream 3: `GET /image.png` 이미지 요청
    - Stream 5: `GET /style.css` CSS 요청
- 각 스트림은 독립적으로 데이터를 주고받으며, 앞선 스트림이 지연되어도 다른 스트림에 영향을 주지 않음

### 3) h2 / h2c 개념

- h2: HTTPS(TLS) 위에서 동작하는 HTTP/2. 대부분의 브라우저 환경에서 사용됨
- h2c: HTTP/2 Cleartext. TLS 없이 일반 HTTP 위에서 동작하는 HTTP/2. 주로 내부 서버 간 통신이나 리버스 프록시와 백엔드 서버 간 통신에서 사용됨

---

### 3. HTTP/1.1 vs HTTP/2 비교

---

| **항목** | **HTTP/1.1** | **HTTP/2** |
| --- | --- | --- |
| **데이터 형식** | 텍스트 | 바이너리 |
| **요청 처리** | 순차 처리 (HOLB 발생) | 동시 처리 (멀티플렉싱) |
| **연결 수** | 요청마다 여러 개 필요 | 하나의 TCP 연결 |
| **헤더 압축** | 없음 | HPACK (압축) |
| **성능** | 상대적으로 느림 | 더 빠름 |
| **브라우저 최적화** | 제한적 | 매우 좋음 (웹 성능 향상) |

---

### 4. 실무 문제 예시: Spring (Java HttpClient) <-> FastAPI (Uvicorn) 간 HTTP/2 충돌

### 1) 문제 상황

Spring Boot 애플리케이션(클라이언트, Java HttpClient)이 FastAPI 애플리케이션(서버, Uvicorn)으로 HTTP 요청을 보낼 때, 

**요청 본문(Request Body)** 이 누락되는 현상이 발생

### 2) 발생 원인 분석

- **Spring (Java HttpClient)의 HTTP/2 Upgrade** 시도:
    - Java `HttpClient`는 기본적으로 HTTP/2를 선호하며, 서버가 HTTP/2를 지원하는지 확인하기 위해 `h2c` (HTTP/2 Cleartext)로의 업그레이드를 시도함
    - 이때 요청 헤더에 `Connection: Upgrade, HTTP2-Settings`와 `Upgrade: h2c`를 포함하여 보냄
- **FastAPI (Uvicorn/ASGI)의 h2c 처리 미흡**:
    - FastAPI 자체의 문제가 아니라, FastAPI가 사용하는 ASGI(Asynchronous Server Gateway Interface) 서버인 Uvicorn의 `h2c` 업그레이드 처리, 특히 **요청 본문 스트리밍 처리** 로직이 완벽하게 구현되지 않았을 때 발생함
    - 클라이언트(Spring)는 `h2c`로 업그레이드된 연결에서 요청 본문을 스트리밍 방식으로 보내지만, 서버(Uvicorn)가 이를 제대로 파싱하지 못하고 본문을 비워버리는 현상이 발생함
- **결과**: FastAPI는 Pydantic 기반의 유효성 검사를 수행하는데, 요청 본문이 비어있으므로 `field required`와 함께 `422 Unprocessable Entity` 에러를 반환함

### 3) 실무적 해결 방안

- 클라이언트 측 (Spring)에서 HTTP/1.1 강제:
    - 가장 빠르고 간단한 해결책. `HttpClient`를 생성할 때 명시적으로 HTTP/1.1만 사용하도록 설정함

    ```java
    HttpClient client = HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1) // HTTP/1.1 강제
                                .build();
    ```

- **서버 측 (FastAPI) 앞에 리버스 프록시 (Nginx, Envoy) 배치**:
    - 가장 권장되는 실무적인 해결책. Nginx나 Envoy 같은 리버스 프록시가 클라이언트로부터 HTTP/2 요청을 받아 처리(Terminate)하고, 백엔드(FastAPI)로는 HTTP/1.1로 변환하여 전달
    - 이 방식은 백엔드 서버가 `h2c` 처리 로직을 신경 쓸 필요 없이 안정적으로 동작하게 함
    - Nginx 설정 예시:

        ```java
        server {
            listen 80;
            # HTTP/2 지원 활성화 (h2c)
            listen 443 ssl http2; # HTTPS 사용 시
            
            location / {
                proxy_pass http://fastapi_backend; # FastAPI 서버 주소
                proxy_http_version 1.1; # 백엔드로는 HTTP/1.1로 전달
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                # ... 기타 프록시 설정
            }
        }
        ```

- **ASGI 서버 업데이트 또는 변경**:
    - Uvicorn이나 다른 ASGI 서버의 버전을 최신으로 업데이트하여 `h2c` 관련 버그가 수정되었는지 확인함. 또는 `Hypercorn` 등 다른 ASGI 서버를 고려해볼 수 있음

---

### 요약
> **HTTP/1.1은 순차 처리의 한계가 명확하고, HTTP/2는 멀티플렉싱을 통해 성능을 비약적으로 개선했음.** 실무에서는 클라이언트와 서버 간의 HTTP 버전 호환성, 특히 h2c와 같은 업그레이드 메커니즘에서 예상치 못한 문제가 발생할 수 있음