---
title: "Scale Up vs Scale Out"
tags:
  - "Cloud"
  - "Scaling"
---
# Scale Up vs Scale Out

## 개요

서비스의 트래픽이 증가하면 서버의 CPU, Memory 등의 자원이 부족해지면서 응답 지연이나 처리 실패가 발생할 수 있음

이때 시스템의 처리 능력을 늘리는 것을 **Scaling(확장)** 이라고 함

- **Scale Up(Vertical Scaling)**: 서버 한 대의 성능을 높이는 방식
- **Scale Out(Horizontal Scaling)**: 서버의 수를 늘리는 방식

## Scale Up (Vertical Scaling)

**기존 서버의 하드웨어 성능을 높이는 방식**

예를 들어 EC2 Instance의 CPU와 Memory를 더 높은 사양으로 변경하는 것
```
Before

EC2
CPU 2 Core
RAM 4GB

        ↓ Scale Up

After

EC2
CPU 8 Core
RAM 32GB
```

### 장점
- 서버 수가 증가하지 않아 구조가 단순함
- 애플리케이션 구조를 크게 변경하지 않아도 됨
- Load Balancer나 분산 처리를 고려할 필요가 적음
- 빠르게 성능을 높일 수 있음

### 단점
- 서버가 제공할 수 있는 성능에는 물리적인 한계가 있음
- 고사양 서버일수록 비용이 크게 증가할 수 있음
- 서버 한 대에 의존하면 장애 발생 시 전체 서비스가 영향을 받을 수 있음
- Instance Type 변경 과정에서 재시작 등의 중단이 발생할 수 있음

Scale Up은 **간단하지만 언젠가는 확장 한계에 도달함**

## Scale Out (Horizontal Scaling)

**서버 한 대의 성능을 높이는 대신 서버의 수를 늘려 요청을 분산하는 방식**

```
Before

Client
  ↓
EC2

        ↓ Scale Out

Client
  ↓
Load Balancer
 ├─ EC2
 ├─ EC2
 └─ EC2
```

AWS에서는 주로 **Load Balancer + 여러 EC2 Instance** 형태로 구성

### 장점
- 트래픽 증가에 따라 서버를 계속 추가할 수 있음
- 일부 서버에 장애가 발생해도 다른 서버가 요청을 처리할 수 있음
- 서버를 실행한 상태에서 새로운 서버를 추가할 수 있어 확장에 유리함
- Cloud Auto Scaling과 잘 맞음

### 단점
서버가 여러 대가 되기 때문에 시스템 구조가 복잡해짐

대표적으로
- Load Balancing
- Session 관리
- 데이터 공유
- 서버 간 상태 관리
- 장애 서버 감지

## Scale Out과 Stateless

Scale Out에서 특히 중요한 개념이 **Stateless**

예를 들어 로그인 Session을 각 Application Server의 Memory에 저장한다고 가정
```
사용자 로그인
    ↓
EC2-1
Session 저장
```
다음 요청이 Load Balancer에 의해 EC2-2로 전달될 수 있음
```
사용자 다음 요청
      ↓
Load Balancer
      ↓
EC2-2

→ EC2-2에는 Session 정보가 없음
```

서버마다 서로 다른 상태를 가지고 있으면 Scale Out이 어려워짐

따라서 Application Server는 가능한 한 상태를 직접 가지고 있지 않는 **Stateless 구조**로 설계하는 것이 유리함

대표적인 방법
```
Application Server
├─ EC2-1
├─ EC2-2
└─ EC2-3
       │
       ↓
공용 상태 저장소
   Redis 등
```

예를 들어 Session을 Redis와 같은 외부 저장소에 저장하면 어떤 서버가 요청을 처리하더라도 동일한 Session을 조회할 수 있음

JWT처럼 서버에 로그인 상태를 저장하지 않는 방식도 Stateless 구조를 만드는 방법 중 하나

## Cloud와 Scale Out

On-Premise에서는 서버를 추가하려면 새로운 물리 서버를 구매하고 설치해야 했음
```
트래픽 증가
→ 서버 구매
→ 배송
→ 설치
→ 설정
```

Cloud에서는 EC2 Instance를 빠르게 생성할 수 있음
```
트래픽 증가
→ EC2 추가
→ Load Balancer 연결
```

AWS에서는 **EC2 Auto Scaling**을 사용하여 트래픽이나 서버 부하에 따라 Instance 수를 자동으로 증가하거나 감소시킬 수도 있음
```
트래픽 증가
    ↓
Auto Scaling
    ↓
EC2 추가

트래픽 감소
    ↓
Auto Scaling
    ↓
EC2 제거
```

이처럼 부하에 따라 자원을 늘리고 줄이는 특성을 **Elasticity(탄력성)** 라고 함

## Scale Up vs Scale Out

| 구분            | Scale Up       | Scale Out   |
| ------------- | -------------- | ----------- |
| 방식            | 서버 성능 증가       | 서버 수 증가     |
| 예시            | CPU 2 → 8 Core | EC2 1대 → 5대 |
| 구조            | 비교적 단순         | 복잡          |
| 확장 한계         | 명확한 한계 존재      | 상대적으로 높음    |
| 장애 대응         | 상대적으로 불리       | 유리          |
| Load Balancer | 일반적으로 불필요      | 일반적으로 필요    |
| Stateless 고려  | 중요도 낮음         | 중요          |
| Cloud 활용      | 가능             | 매우 적합       |

## 헷갈리는 포인트

### Scale Up을 사용하면 안되는가?

아님. 규모가 크지 않은 서비스나 DB처럼 Scale Out이 복잡한 시스템에서는 Scale Up이 간단하고 효과적인 선택일 수 있음

중요한 것은 무조건 Scale Out을 사용하는 것이 아니라 **서비스의 규모와 요구사항에 맞게 선택하는 것**

### Scale Out하면 자동으로 고가용성이 되는가?

아님. 서버를 여러 대 운영하더라도 모두 같은 AZ에 있거나 Load Balancer 자체의 설정이 잘못되어 있다면 장애에 취약할 수 있음

Scale Out은 고가용성을 만들기 위한 하나의 수단

### Scale Out하면 서버 수를 무한히 늘릴 수 있는가?

아님. Application Server는 비교적 쉽게 확장할 수 있지만 DB, Cache, 외부 API 등 다른 시스템이 새로운 병목이 될 수도 있음

## 핵심 정리

### Scale Up
- 기존 서버의 CPU, Memory 등 성능 증가
- 구조가 단순함
- 물리적인 확장 한계가 존재

### Scale Out
- 서버의 수를 증가
- Load Balancer를 통해 트래픽 분산
- 확장성과 장애 대응에 유리
- Stateless 설계와 분산 환경 고려 필요

```
Scale Up
= 서버 한 대를 더 강하게

Scale Out
= 서버 여러 대로 나누어서 처리
```

> **Cloud에서는 서버를 빠르게 생성하고 제거할 수 있기 때문에 Scale Out과 Auto Scaling을 활용하기 좋음**
