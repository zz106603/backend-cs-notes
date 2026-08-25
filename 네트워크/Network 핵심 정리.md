---
title: "Network 핵심 정리"
tags:
  - "Network"
  - "네트워크"
  - "정리"
---
# Network 핵심 정리

## 네트워크

백엔드 애플리케이션은 대부분 네트워크를 통해 요청을 받고 다른 시스템과 통신함

사용자가 API를 호출하면 단순히 Spring Controller가 바로 실행되는 것이 아니라 대략 다음 과정을 거침

```
Client
  ↓
DNS
  ↓
TCP 연결
  ↓
TLS 연결 (HTTPS)
  ↓
HTTP Request
  ↓
Load Balancer / Reverse Proxy
  ↓
Spring Boot
```

백엔드 개발자는 최소한 **요청이 서버까지 어떻게 전달되는지**, 그리고 문제가 발생했을 때 **어느 구간을 확인해야 하는지** 이해해야 함

## IP와 Port

### IP
**IP는 네트워크에서 장치를 식별하기 위한 주소**
```
192.168.0.10
```

하지만 하나의 서버에서는 여러 프로그램이 동시에 실행될 수 있음
```
Spring Boot
MySQL
Redis
Nginx
```
이때 어떤 프로그램과 통신할지를 구분하기 위해 Port를 사용

### Port
**Port는 하나의 장치 안에서 어떤 프로세스와 통신할지를 구분하는 번호**
| 서비스        | 기본 Port |
| ---------- | ------: |
| HTTP       |      80 |
| HTTPS      |     443 |
| MySQL      |    3306 |
| PostgreSQL |    5432 |
| Redis      |    6379 |

예를 들어 `192.168.0.10:8080` 이라면 `192.068.0.10`은 어느 서버인가, `8080`은 그 서버의 어떤 프로그램인가

## TCP와 UDP
TPC와 UDP는 데이터를 전달하는 대표적인 **전승 계층 프로토콜**

### TCP
**TCP는 상대방과 연결을 맺은 후 데이터를 전달**

특징
- 연결 지향
- 데이터 전달 보장
- 순서 보장
- 손실 시 재전송
- UDP보다 상대적으로 오버헤드가 큼

HTTP/1.1과 HTTP/2는 기본적으로 TCP 위에서 동작함

`HTTP -> TCP -> IP`

### UDP
**UDP는 별도의 연결 과정 없이 데이터를 전달**

특징
- 연결 과정 없음
- 전달 여부 보장 없음
- 순서 보장 없음
- 재전송을 기본 제공하지 않음
- 오버헤드가 작음

DNS나 실시간 통신 등에서 사용됨

HTTP/3는 UDP 기반 프로토콜인 **QUIC**을 사용함

### TCP vs UDP
| TCP       | UDP       |
| --------- | --------- |
| 연결 필요     | 연결 없음     |
| 신뢰성 높음    | 신뢰성 보장 없음 |
| 순서 보장     | 순서 보장 없음  |
| 재전송 지원    | 기본적으로 없음  |
| 상대적으로 무거움 | 상대적으로 가벼움 |

## TCP 3-Way Handshake
TCP는 데이터를 보내기 전에 **클라이언트와 서버 사이에 연결을 만듬**

```
Client                    Server

  ───── SYN ─────────────→

  ←──── SYN + ACK ────────

  ───── ACK ─────────────→

       연결 완료
```

### 1. SYN
- 클라이언트가 서버에 연결을 요청

### 2. SYN + ACK
- 서버가 요청을 받았으면 자신도 통신할 준비가 되었음을 알림

### 3. ACK
- 클라이언트도 서버의 응답을 정상적으로 받았음을 알림

양쪽이 서로 **송수신 가능한 상태인지 확인한 뒤 연결을 만드는 과정**

## TCP 연결 종료
TCP 연결을 종료할 때는 일반적으로 4-Way Handshake를 사용
```
Client                    Server

  ───── FIN ─────────────→
  ←──── ACK ──────────────
  ←──── FIN ──────────────
  ───── ACK ─────────────→
```
연결할 때와 달리 양쪽이 데이터 전송을 끝내는 시점이 다를 수 있기 때문에 각각 종료를 확인

## DNS
사용자는 서버 IP를 직접 입력하기보다는 **도메인**을 사용함
```
api.example.com
```

하지만 실제 네트워크 통신에서는 IP 주소가 필요함. DNS는 도메인을 IP 주소로 변환
```
api.example.com
       ↓
      DNS
       ↓
203.0.113.10
```
즉, DNS는 쉽게 말해 **도메인의 IP 주소를 찾아주는 시스템**

## HTTP
HTTP는 클라이언트와 서버가 데이터를 주고받기 위한 **애플리케이션 계층 프로토콜**
```
Client
   ↓ HTTP Request
Server
   ↓ HTTP Response
Client
```
HTTP의 중요한 특징 중 하나는 **Stateless**

### Stateless
HTTP 자체는 이전 요청의 상태를 기억하지 않음
```
1번 요청 → 로그인
2번 요청 → 사용자 정보 조회
```
HTTP 자체만 놓고 보면 1, 2번 요청이 같은 사용자의 요청인지 알 수 없음

그래서 로그인 상태를 유지하기 위해 Session, Cookie, JWT 등의 방식을 사용

## HTTP Request / Response

### Request
HTTP 요청은 크게 다음으로 구성
```
Request Line
Header
Body
```

example:
```http
POST /users HTTP/1.1
Host: example.com
Content-Type: application/json
{
  "name": "yun"
}
```

### Response
```
HTTP/1.1 200 OK
Content-Type: application/json
{
  "id": 1,
  "name": "yun"
}
```

## HTTP Method
HTTP Method는 요청의 목적을 표현
| Method | 용도       |
| ------ | -------- |
| GET    | 조회       |
| POST   | 생성 또는 처리 |
| PUT    | 전체 수정    |
| PATCH  | 일부 수정    |
| DELETE | 삭제       |

example:
```
GET /users/1
POST /users
PATCH /users/1
DELETE /users/1
```

## HTTP Status Code

### 2xx
정상 처리
- `200 OK`
- `201 Created`
- `204 No Content`

### 4xx
클라이언트 요청 문제
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### 401 vs 403
401(인증): 인증이 필요한데 인증되지 않은 상태

403(권한): 요청을 수행할 권한이 없는 상태

### 5xx
서버 측 문제
- `500 Internal Server Error`
- `502 Bad Gateway`
- `503 Service Unavailable`
- `504 Gateway Timeout`

## HTTP와 HTTPS
HTTP는 기본적으로 데이터를 암호화하지 않음

HTTPS는 HTTP에 **TLS(Transport Layer Security)** 를 적용해 통신 내용을 보호

```
HTTP
 ↓
TLS
 ↓
TCP
 ↓
IP
```
주요 목적
- 데이터 암호화
- 데이터 위변조 방지
- 서버 신원 확인

## TLS와 인증서
HTTPS 연결 시 서버는 인증서를 제공

인증서 정보
- 서버 도메인
- 서버 공개키
- 인증기관 정보
- 전사 서명

문제:
- 공격자가 자신의 공개키를 서버의 공개키인 것처럼 보낼 수도 있음

그래서 **CA(Certificate Authority)** 가 필요 
```
Server Certificate
       ↓
CA의 서명 확인
       ↓
신뢰할 수 있는 서버인지 판단
```

## Cookie
Cookie는 **브라우저가 저장하는 작은 데이터**

서버가 다음과 같이 응답하면 `Set-Cookie: SESSION_ID=abc123`

브라우저가 값을 저장하고 이후 요청에 포함할 수 있음 `Cookie: SESSION_ID=abc123`

중요한 점은 **Cookie 자체가 인증 방식은 아니라는 것**

Cookie는 단순히 클라이언트가 값을 저장하고 서버에 전달하는 방법

## Session
Session 방식에는 **로그인 상태를 서버가 관리**
```
Client
  ↓ 로그인
Server
  ↓
Session 생성

Client ← Session ID
```

이후 클라이언트는 일반적으로 Cookie를 통해 Session ID를 전달
```
Cookie: JSESSIONID=abc123
```

서버는 Session ID를 이용해 사용자를 찾음
```
Session ID
   ↓
Session Store
   ↓
User 정보
```

따라서 일반적인 웹 Session 인증은 `Cookie + Session` 구조

## JWT
JWT 방식에서는 **서버가 로그인 성공 후 Token을 발급**
```
Client
  ↓ 로그인
Server
  ↓
JWT 발급
  ↓
Client
```

이후 요청할 때 JWT를 전달
```
Authorization: Bearer eyJhbGciOi...
```
서버는 JWT의 서명을 검증해 유효한 Token인지 판단

### Session vs JWT

**Session**
```
Client
  ↓ Session ID
Server
  ↓
Session 저장소에서 사용자 확인
```
로그인 상태를 서버가 관리

**JWT**
```
Client
  ↓ JWT
Server
  ↓
JWT 검증
```
토큰을 이용해 사용자를 판단

JWT가 Session보다 무조건 좋은 것은 아님

**웹 기반 단일 서버 구조에서는 Session**이 단순할 수 있고, **여러 서비스가 인증 정보를 공유하는 구조에서는 Token** 방식이 유리할 수 있음

## Proxy와 Reverse Rroxy

### Proxy
**클라이언트 앞에 위치해 클라이언트를 대신해 요청**
```
Client
  ↓
Proxy
  ↓
Server
```
서버는 실제 클라이언트가 아니라 Proxy로부터 요청을 받음

### Reverse Proxy
**서버 앞에 위치**
```
Client
  ↓
Nginx
  ↓
Spring Boot
```
주요 역할
- 요청 전달
- HTTPS 처리
- Load Balancing
- 캐싱
- 실제 서버 은닉

Nginx가 대표적인 Reverse Proxy

## Load Balancing
요청이 많아지면 서버 하나만으로 모든 요청을 처리하기 어려움
```
              ┌─ Server 1
Client → LB ─┼─ Server 2
              └─ Server 3
```
Load Balancer는 요청을 여러 서버에 분산
- 부하 분산
- Scale Out
- 장애 대응
- 가용성 향상

## HTTP 요청이 Spring Boot까지 도착하는 과정

다음 API 호출한다고 가정 `https://api.example.com/users`

### 1. DNS 조회
```
api.example.com
      ↓
203.0.113.10
```
도메인을 IP 주소로 변환

### 2. TCP 연결
3-Way Handshake를 통해 연결
```
SYN
SYN + ACK
ACK
```

### 3. TLS 연결
HTTPS라면 인증서를 확인하고 암호화 통신에 필요한 정보를 교환

### 4. HTTP Request 전송
```
GET /users HTTP/1.1
Host: api.example.com
```

### 5. Load Balancer / Reverse Proxy
요청을 실제 애플리케이션 서버로 전달
```
Client
  ↓
ALB / Nginx
  ↓
Spring Boot
```

### 6. Spring Boot 처리
이후 Spring 내부에서는 대략 다음 흐름으로 처리
```
Tomcat
  ↓
Filter
  ↓
DispatcherServlet
  ↓
Interceptor
  ↓
Controller
  ↓
Service
  ↓
Repository
```

## Network에서 우선 순위 질문

### TCP와 UDP의 차이는?
TCP는 연결을 맺고 데이터의 전달과 순서를 보장하는 반면, UDP는 이러한 보장을 하지 않는 대신 더 가볍게 데이터를 전달

### 3-Way Handshake는 왜 필요한가?
클라이언트가 서버가 서로 데이터를 송수신할 수 있는 상태인지 확인하고 TCP 연결을 만들기 위해 필요함

### DNS?
도메인을 실제 통신에 필요한 IP 주소로 변환하는 시스템

### HTTP가 Stateless하다는 것은?
각 HTTP 요청이 독립적이며 HTTP 자체가 이전 요청의 상태를 기억하지 않는다는 의미

### HTTP와 HTTPS 차이는?
HTTPS는 HTTP에 TLS를 적용해 통신 내용을 암호화하고 서버의 신원을 검증

### Cookie와 Session의 차이는?
- Cookie는 클라이언트에 값을 저장하고 전달하는 방식
- Session은 서버가 사용자의 상태를 관리하는 방식

Session ID를 Cookie에 저장하는 형태로 함께 사용되는 경우가 많음

### Session과 JWT의 차이는?
Session은 로그인 상태를 주로 서버에서 관리하고, JWT 방식은 클라이언트가 Token을 보관해 요청마다 서버로 전달

### Reverse Proxy는 왜 사용?
클라이언트와 애플리케이션 서버 사이에서 요청 전달, HTTPS 처리, Load Balancing, 캐싱 등의 역할을 수행하기 위해 사용

## 핵심 흐름
Network 문서에서 가장 중요한 것은 개별 용어를 전부 암기하는 것이 아니라 아래 흐름을 이해하는 것
```
URL 입력 / API 호출
       ↓
DNS로 IP 확인
       ↓
TCP 연결
       ↓
HTTPS라면 TLS 연결
       ↓
HTTP Request
       ↓
Reverse Proxy / Load Balancer
       ↓
Spring Boot
       ↓
HTTP Response
```
