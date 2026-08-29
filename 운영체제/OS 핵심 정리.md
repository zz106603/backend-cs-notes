---
title: "OS 핵심 정리"
tags:
  - "OS"
  - "운영체제"
  - "정리"
---
# OS 핵심 정리

## 운영체제

백엔드 애플리케이션은 운영체제 위에서 실행됨

Java/Spring Boot 기준으로 보면 대략 다음 구조

```
Hardware
   ↓
Operating System
   ↓
JVM Process
   ↓
Java Thread
   ↓
Spring Boot 요청 처리
```

최소한의 이해
- Process와 Thread 차이
- 여러 요청이 동시에 처리되는 원리
- CPU가 여러 작업을 처리하는 방식
- Heap과 Stack의 차이
- 동시 접근 시 문제가 발생하는 이유
- Blocking / Non-Blocking
- Deadlock이 발생하는 이유

## Program과 Process

### Program
Program은 실행되기 전의 코드
```
my-app.jar
```
파일 자체는 아직 실행 중인 작업이 아님

### Process
Program을 실행하면 운영체제가 실행에 필요한 자원을 할당하고 Process를 만듬
```
java -jar app.jar
       ↓
Java Process
```
일반적으로 다음과 같은 자원을 가짐
- Memory
- CPU 실행 상태
- File Descriptor
- Thread
- 운영체제가 관리하는 Process 정보

즉, 
```
Program = 실행 파일
Process = 실행 중인 프로그램
```

## Process와 Thread
Process 안에는 실제 코드를 실행하는 **Thread**가 존재
```
Process
 ├─ Thread 1
 ├─ Thread 2
 ├─ Thread 3
 └─ Thread 4
```
Process는 실행 환경과 자원의 단위이고, Thread는 **실제 실행 흐름의 단위**

### Process 간 메모리
각 Process는 기본적으로 독립적인 메모리 공간을 가짐
```
Process A            Process B
Memory A             Memory B
```
다른 Process의 메모리에 직접 접근할 수 없음

Process 간 데이터를 주고받으려면 별도의 IPC가 필요
-Pipe
Socket
Shared Memory

### Thread 간 메모리
같은 Process의 Thread들은 일부 메모리를 공유
```
Process

          Heap
           ↑
 ┌─────┼─────┐
Thread1 Thread2 Thread3
```
각 Thread는 자신만의 Stack을 가지지만 Process의 Heap 등을 공유

이 때문에 Thread 간 데이터 공유는 쉽지만 **동시성 문제도 발생할 수 있음**

## Process vs Thread
| Process                   | Thread                            |
| ------------------------- | --------------------------------- |
| 실행 중인 프로그램의 단위            | Process 내부의 실행 흐름                 |
| 독립적인 메모리 공간               | Heap 등 일부 메모리 공유                  |
| 생성 비용이 상대적으로 큼            | 상대적으로 가벼움                         |
| 다른 Process와 직접 메모리 공유 어려움 | 같은 Process 내 데이터 공유 쉬움            |
| 장애 격리가 상대적으로 좋음           | 한 Thread의 문제가 Process에 영향을 줄 수 있음 |

## Spring Boot와 Thread
Spring Boot 애플리케이션도 하나의 Process로 실행됨
```
Spring Boot Process
       ↓
     Tomcat
       ↓
 Thread Pool
```
여러 HTTP 요청이 들어오면 Tomcat의 여러 Thread가 요청을 처리할 수 있음
```
Request A → Thread 1
Request B → Thread 2
Request C → Thread 3
```
따라서 Spring Boot는 하나의 Process지만 **여러 요청을 동시에 처리할 수 있음**

이 구조 때문에 Spring Bean이 공유하는 데이터에 여러 Thread가 동시에 접근하면 문제가 발생할 수 있음

## CPU와 Scheduling
하나의 CPU Core는 기본적으로 한 순간에 하나의 Thread를 실행

컴퓨터를 사용할 때 많은 프로그램이 동시에 실행되는 것처럼 보이는 것은 운영체제가 CPU 사용 시간을 빠르게 나눠주기 때문
```
Thread A 실행
   ↓
Thread B 실행
   ↓
Thread C 실행
   ↓
Thread A 실행
```
어떤 Process나 Thread에게 CPU를 할당할지 결정하는 것을 **Scheduling**이라고 함

## Context Switching
CPU가 실행 중인 Thread를 바꾸려면 기존 Thread의 상태를 저장하고 새로운 Thread의 상태를 불러와야 함

```
Thread A 실행
      ↓ 상태 저장
Thread B 실행
      ↓
Thread A 상태 복구
```
대표적으로 다음 정보가 포함
- Program Counter
- Register
- Stack 관련 상태

### 비용
Context Switching 자체는 실제 비즈니스 작업을 수행하는 것이 아님

Thread가 지나치게 많으면 CPU 비용이 증가할 수 있음
```
Thread A 실행

      ↓ 상태 저장

Thread B 실행

      ↓

Thread A 상태 복구
```
**Thread가 많으면 무조건 빠르다는 것은 잘못된 생각**

Thread Pool의 크기도 무작정 크게 설정하면 안 되는 이유

## 동시성과 병렬성

### Concurrency - 동시성
여러 작업을 번갈아 처리해서 동시에 진행되는 것처럼 만드는 것
```
CPU 1개
A → B → A → C → B → A
```

### Parallelism - 병렬성
실제로 여러 CPU Core에서 동시에 실행하는 것
```
Core 1 → Task A
Core 2 → Task B
Core 3 → Task C
```

즉,
```
Concurrency
= 여러 작업을 함께 진행하는 구조

Parallelism
= 여러 작업을 실제로 동시에 실행
```

## Memory 구조
Process 메모리는 개념적으로 다음과 같은 영역으로 구분
```
Process Memory

┌────────┐
│   Stack     │
├────────┤
│      ↓     │
│             │
│      ↑     │
├────────┤
│    Heap     │
├────────┤
│    Data     │
├────────┤
│    Code     │
└────────┘
```

### Code 영역
실행할 Program의 코드가 저장

### Data 영역
전역 변수나 정적 데이터 등이 저장

### Heap 영역
**동적으로 생성되는 데이터가 저장**

Java에서는 객체가 주로 Heap에 생성
```Java
User user = new User();
```
`new User()`로 생성한 객체는 JVM Heap에서 관리

### Stack 영역
**함수 호출과 관련된 지역 변수, 매개변수, 반환 주소 등이 저장**

Thread마다 독립적인 Stack이 존재
```
Thread 1 → Stack 1
Thread 2 → Stack 2
       ↓
      Heap 공유
```

## Stack과 Heap

### Stack
- Thread마다 독립적
- 함수 호출과 함께 생성
- 함수 종료 시 제거
- 접근 속도가 빠름
- 크기가 제한적
```Java
public void test() {
    int count = 10;
}
```

### Heap
- Process 내에서 공유되는 영역
- 동적으로 생성되는 객체 저장
- 여러 Thread가 접근 가능
- Java에서는 GC가 관리
```Java
User user = new User();
```

### Thread Safety 문제 발생 이유
Thread의 Stack은 서로 독립적이기 때문에 일반적으로 직접 충돌하지 않음

하지만 Heap의 객체는 여러 Thread가 공유할 수 있음
```
Thread A ─┐
          ↓
      Shared Object
          ↑
Thread B ─┘
```
그래서 여러 Thread가 동시에 같은 객체의 상태를 수정하면 **Race Condition**이 발생할 수 있음

## Virtual Memory
운영체제는 각 Process가 자신만의 연속된 메모리를 가지고 있는 것처럼 보이게 함

```
Process A

0x0000
0x0001
...
```
Process가 사용하는 주소는 실제 RAM 주소와 바로 같은 것은 아님

운영체제가 변환
```
Virtual Address
       ↓
Physical Address
```

### 사용 이유

### 1. Process 격리
Process A가 Process B의 메모리에 마음대로 접근하는 것을 막음

### 2. 메모리 관리 단순화
각 Process가 독립적인 주소 공간을 가진 것처럼 사용할 수 있음

### 3. 실제 RAM보다 큰 주소 공간 활용
사용하지 않는 메모리 일부를 Disk 등으로 이동시킬 수 있음

## Paging
Virtual Memory는 일반적으로 메모리를 일정 크기의 단위로 나누어 관리함

Virtual Memory에서는 **Page**, Physical Memory에서는 **Frame**이라고 부름
```
Virtual Memory
Page 1
Page 2
Page 3
  ↓
Physical Memory
Frame 5
Frame 1
Frame 8
```
Page Table을 이용해 Virtual  Page가 어느 Physical Frame이 있는지 관리함

## Page Fault
Process가 사용하려는 Page가 현재 RAM에 없으면 **Page Fault**가 발생
```
CPU
 ↓
Page 접근
 ↓
RAM에 없음
 ↓
Page Fault
 ↓
Disk에서 가져옴
 ↓
RAM에 적재
```
Disk 접근은 RAM보다 매우 느리기 때문에 Page Fault가 지나치게 많이 발생하면 성능이 크게 떨어짐

## User Mode와 Kernel Mode
운영체제는 프로그램이 시스템의 모든 기능에 접근하지 못하도록 실행 권한을 구분

### User Mode
일반 Application이 실행되는 영역
```
Spring Boot
Java Application
Browser
```
Hardware나 중요한 OS 자원에 직접 접근할 수 없음

### Kernel Mode
운영체제 Kernel이 실행되는 영역
- File 접근
- Network 처리
- Memory 관리
- Process 관리
- Hardware 제어

## System Call
User Mode의 Application이 운영체제 기능을 사용하려면 Kernel에게 요청해야 함

```Java
Files.readString(path);
```
```
Java Application
      ↓
System Call
      ↓
Operating System
      ↓
Disk
```
개념적으로 위와 같은 과정이 필요함

Network Socket 역시 결국 OS의 System Call을 이용함

## Race Condition
여러 Thread가 공유 데이터를 동시에 변경하면서 실행 순서에 따라 결과가 달라지는 현상

`count = 0인 값을 두 Thread가 동시에 count++`
```
1. count 읽기
2. +1
3. count 저장
```
```
Thread A → count 0 읽음
Thread B → count 0 읽음

Thread A → 1 저장
Thread B → 1 저장
```
예상 값은 2, 실제 값은 1

## Critical Section
여러 Thread가 동시에 접근하면 **문제가 발생하는 코드 영역**
```Java
count++;
```
공유 데이터를 변경하는 부분이 대표적

Critical Section에서는 한 번에 하나의 Thread만 작업하도록 제어가 필요함

## Mutex
**한 번에 하나의 Thread**만 Critical Section에 접근하도록 하는 동기화 방식
```
Thread A
   ↓
Mutex Lock 획득
   ↓
Critical Section
   ↓
Mutex 해제

Thread B
   ↓
대기
```

## Semaphore
동시에 접근할 수 있는 **Thread의 개수를 제한**

Semaphore 값이 3이라면
```
Thread A → 접근 가능
Thread B → 접근 가능
Thread C → 접근 가능
Thread D → 대기
```
즉, Mutext가 보통 1명만 허용한다면 Semaphore는 **정해진 N명까지 허용**

## Mutex vs Semaphore
| Mutex           | Semaphore       |
| --------------- | --------------- |
| 일반적으로 1개 Thread | N개 Thread 허용 가능 |
| 상호 배제 목적        | 동시 접근 수 제한      |
| Lock 소유 개념      | Permit 개념       |

## Deadlock
여러 Thread나 Process가 서로가 가진 자원을 기다리면서 더 이상 진행할 수 없는 상태
```
Thread A
→ Resource 1 획득
→ Resource 2 대기

Thread B
→ Resource 2 획득
→ Resource 1 대기
```
```
A → B 기다림
↑         ↓
└───── B
```

## Deadlock 발생 조건

### 상호 배제
한 번에 하나의 작업만 자원을 사용할 수 있음

### 점유와 대기
이미 하나의 자원을 가진 상태로 다른 자원을 기다림

### 비선점
다른 작업이 가진 자원을 강제로 빼앗을 수 없음

### 순환 대기
서로가 서로의 자원을 기다리는 순환 구조가 만들어짐

## Blocking과 Non-Blocking

### Blocking
작업의 결과가 나올 때까지 **Thread가 기다리는 방식**
```
Thread
  ↓
DB Query
  ↓
........ 기다림 ........
  ↓
Result
  ↓
다음 코드 실행
```
Spring MVC + JDBC는 일반적으로 이런 Blocking 방식으로 동작

### Non-Blocking
작업이 바로 완료되지 않더라도 Thread를 계속 묶어두지 않는 방식
```
Thread
  ↓
I/O 요청
  ↓
다른 작업 수행
  ↓
I/O 완료
```
적은 Thread로 많은 I/O 작업을 처리하는 데 유리할 수 있음

Spring WebFlux가 대표적으로 Non-Blocking I/O 모델을 사용

## Synchronous와 Asynchronous
Blockint/Non-Bloking과 혼동하기 쉬움 개념

### Synchronous
작업 완료를 호출한 쪽의 흐름에서 기다리고 결과를 이어서 처리하는 형태
```
A 요청
  ↓
B 작업
  ↓
B 결과
  ↓
A 다음 작업
```

### Asynchronous
작업 완료를 기다리지 않고 현재 흐름을 계속 진행하고 결과는 나중에 처리
```
A
 ↓
B 작업 요청
 ↓
A 다른 작업 수행

       B 완료
         ↓
      결과 처리
```

### Blocking과 Sync는 같은 말이 아님
```
Blocking / Non-Blocking
→ 호출한 Thread가 기다리는가?

Sync / Async
→ 작업 완료 결과를 어떤 흐름으로 처리하는가?
```

## I/O
Input / Output

백엔드 대표적 I/O
- Database Query
- HTTP API 호출
- File Read / Write
- Redis 접근
- Message Queue 접근

CPU 연산과 달리 외부 장치나 다른 시스템의 응답을 기다리는 시간이 발생
```
Spring Thread
     ↓
DB Query
     ↓
Database 처리 기다림
     ↓
Result
```
백엔드 Application은 CPU 연산보다 이런 I/O Wait가 많은 경우가 많음

## CPU Bound와 I/O Bound

### CPU Bound
CPU 연산이 성능의 핵심인 작업
- 암호화
- 이미지 처리
- 복잡한 계산
- 데이터 압축

### I/O Bound
외부 I/O 대기 시간이 큰 작업
- Database
- Network API
- File
- Redis
일반적인 Web Backend 요청은 I/O Bound인 경우가 많음

## File Descriptor
Linus에서는 File, Socket 등의 I/O Resource를 File Descriptor라는 숫자로 관리

예를 들어 Process가 Network Connection을 만들면 OS는 해당 Socker을 관리할 File Descriptor를 할당
```
Process

FD 0 → Standard Input
FD 1 → Standard Output
FD 2 → Standard Error
FD 3 → File
FD 4 → Socket
FD 5 → Socket
```

### 왜 알아야 할까
Server가 너무 많은 Connection이나 File을 열고 닫지 않으면 File Descriptor를 모두 사용할 수 있음

대표적인 오류:
```
Too many open files
```
즉, Application Resource를 정상적으로 닫는 것도 중요

## Spring Boot와 OS 연결
```
Operating System
       ↓
Java Process (JVM)
       ↓
Tomcat Thread Pool
       ↓
Thread
       ↓
HTTP Request 처리
       ↓
Database / Redis / API
       ↓
I/O 대기
```
여러 요청이 들어와 모든 Thread가 DB 응답을 오래 기다리면 Tomcat Thread Pool이 모두 사용될 수 있음

그러면 새로운 요청이 들어와도 처리할 Thread가 없어 요청이 대기
```
Slow DB
   ↓
Thread 장시간 점유
   ↓
Thread Pool 부족
   ↓
새 요청 대기
   ↓
API 전체 지연
```

## 정리

### Process와 Thread의 차이는?

Process는 실행중인 프로그램과 그 자원을 관리하는 단위이고, Thread는 Process 내부에서 실제 코드를 실행하는 흐름

같은 Process의 Thread들은 Heap 등의 자원을 공유함

### Thread를 많이 만들면 성능이 좋아지는가?

아님. Thread가 많으면 동시에 많은 작업을 처리할 수 있지만 Context Switching과 Memory 사용량도 증가함

따라서 작업 특성과 CPU, I/O 상황에 맞춰 적절하게 사용해야 함

### Context Switching이란?

CPU가 실행 중인 Thread를 변경하기 위해 현재 Thread의 상태를 저장하고 다른 Thread의 상태를 복원하는 과정

### Stack과 Heap의 차이는?

Stack은 Thread마다 독립적으로 존재하며 함수 호출과 지역 변수 등을 관리함

Heap은 객체 등 동적으로 생성된 데이터를 저장하며 여러 Thread가 공유할 수 있음

### Race Condition이란?

여러 Thread가 공유 데이터에 동시에 접근하면서 실행 순서에 따라 결과가 달라지는 현상

### Mutext와 Semaphore의 차이는?

Mutex는 일반적으로 하나의 Thread만 Critical Section에 접근하게 하고, Semephore는 정해진 개수의 Thread가 동시에 자원에 접근할 수 있도록 제한

### Deadlock이란?

여러 Thread나 Process가 서로가 점유한 자원을 기다리면서 더 이상 진행할 수 없는 상태

### Blocking과 Non-Blocking의 차이는?

Bloking은 작업 완료를 기다리는 동안 호출 Thread가 대기

Non-Blocking은 작업이 완료되지 않아도 Thread가 다른 작업을 수행할 수 있도록 하는 방식

## Virtual Memory를 사용하는 이유는?

Process마다 독립적인 주소 공간을 제공해 메모리를 보호하고, Physical Memory를 효율적으로 관리하기 위해 사용

## 핵심 흐름
```
Program 실행
      ↓
Process 생성
      ↓
Process 안에 여러 Thread
      ↓
OS가 Thread Scheduling
      ↓
CPU에서 Thread 실행
      ↓
필요하면 Context Switching
      ↓
Thread들은 Heap 공유
      ↓
공유 데이터 동시 접근
      ↓
Race Condition 가능
      ↓
Mutex / Semaphore 등으로 제어
```
Spring Boot 연결:
```
OS
 ↓
JVM Process
 ↓
Tomcat Thread Pool
 ↓
HTTP Request
 ↓
Java Thread
 ↓
DB / Network I/O
 ↓
응답 대기
 ↓
Thread 반환
```
