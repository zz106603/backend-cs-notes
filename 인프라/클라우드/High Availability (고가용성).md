---
title: "High Availability (고가용성)"
tags:
  - "Cloud"
  - "클라우드"
  - "고가용성"
---
# High Availability (고가용성)

## 개요

서비스 운영에서 중요한 목표 중 하나는 **장애가 발생하더라도 서비스를 계속 제공하는 것**

이를 **High Availability(HA, 고가용성)** 라고 함

> 핵심은 **장애 자체를 없애는 것이 아니라, 장애가 발생해도 서비스 전체가 중단되지 않도록 설계하는 것**

## High Availability

**시스템 일부에 장애가 발생해도 전체 서비스를 계속 제공할 수 있도록 구성하는 것**

목표
- 서비스 중단 최소화
- 장애 영향 범위 축소
- 빠른 복구 및 Failover
- SPOF 제거

## 필요 이유

실제 웅영 환경에서는 장애를 완전히 피할 수 없음

- Application Server 장애
- Database 장애
- Network 장애
- AZ 장애
- Hardware 장애

```
장애가 발생하지 않게 한다
        ↓
현실적으로 불가능

장애가 발생해도
서비스가 유지되게 한다
        ↓
High Availability
```

## SPOF (Single Point of Failure)

**하나의 구성 요소가 장애 났을 때 전체 시스템이 중단되는 지점**

예를 들어 서버가 한 대
```
Client
  ↓
EC2 1대
```

EC2가 장애 나면 전체 서비스가 중단됨

따라서 EC2가 SPOF가 됨

고가용성 설계에서는 이러한 SPOF를 가능한 한 제거

## 기본적인 HA 구조

### 단일 서버
```
Client
  ↓
EC2
```

### 다중 서버
```
             ┌─ EC2
Client → ALB ├─ EC2
             └─ EC2
```

## Load Balancer의 역할

Load Balancer는 여러 서버에 요청을 분산하며 HA 구성에서 중요한 역할을 함

AWS에서는 **Elastic Load Balancing(ELB)** 을 사용할 수 있음

주요 역할
- 요청 분산
- 정상 서버로 트래픽 전달
- 장애 서버 제외
- Health Check 수행

예를 들어
```
GET /health
```
같은 Health Check가 계속 실패하면 해당 서버를 비정상 상태로 판단하고 트래픽 전달을 중단할 수 있음

## HA는 모든 계층에서 고려

서버만 여러 대 만든다고 전체 시스템이 고가용성이 되는 것은 아님
```
Application Server
Database
Network
Availability Zone
```
각 계층에 SPOF가 존재하는지 확인해야 함

### 1. Application 계층
EC2 한 대가 아니라 여러 대를 사용함
```
ALB
├─ EC2
├─ EC2
└─ EC2
```

### 2. Database 계층
Application Server가 여러 대여도 DB가 한 대뿐이라면 DB가 SPOF가 됨

AWS RDS에서는 **Multi-AZ** 구성을 이용할 수 있음
```
AZ A
Primary DB
    │
    │ Replication
    ↓
AZ B
Standby DB
```
Primary DB에 장애가 발생하면 Standby로 전환하여 서비스를 복구할 수 있음

### 3. AZ 계층
서버 여러 대를 모두 같은 AZ에 두면 해당 AZ 전체 장애에는 대응할 수 없음

따라서 여러 AZ에 서버를 분산
```
             ALB
            /   \
           /     \
       AZ A       AZ B
       EC2        EC2
       DB         Standby DB
```
이러한 Multi-AZ 구조를 통해 한 AZ의 장애가 전체 서비스 장애로 이어지는 위험을 줄일 수 있음

## Failover

**Failover는 현재 사용중인 시스템에 장애가 발생했을 때 정상적인 다른 시스템으로 전환하는 과정**

예를 들어
```
Primary DB 장애
      ↓
Standby DB 승격
      ↓
서비스 계속 운영
```
과 같은 과정이 Failover

Failover는 HA를 구현하기 위한 대표적인 방법 중 하나

## HA와 Fault Tolerance의 차이

### High Availability
장애가 발생해도 **빠르게 복구하거나 다른 시스템으로 전환하여 다운타임을 최소화**하는 것을 목표로 함

### Fault Tolerance
일부 구성 요소에 장애가 발생해도 **서비스 중단 없이 계속 동작하는 수준**을 목표로 함

따라서 일반적으로 Fault Tolerance가 더 강한 장애 대응 수준
```
High Availability
→ 장애 발생 후 빠른 복구 / 전환

Fault Tolerance
→ 장애가 발생해도 계속 동작
```

## 고가용성과 무중단
HA를 구성했다고 해서 장애 시 중단 시간이 항상 0초인 것은 아님

Failover 과정에서 짧은 중단이 발생할 수 있음
```
장애 발생
→ 감지
→ Failover
→ 서비스 복구
```

> **High Availability = 다운타임 최소화**

## Availability 수치
서비스의 가용성은 흔히 `99.9%`, `99.99%` 같은 값으로 표현

연간 기준으로 보면 대략 다음과 같음
| 가용성     | 허용되는 연간 다운타임 |
| ------- | -----------: |
| 99%     |      약 3.65일 |
| 99.9%   |     약 8.76시간 |
| 99.99%  |      약 52.6분 |
| 99.999% |      약 5.26분 |

가용성이 높아질수록 허용되는 장애 시간이 급격히 줄어듬

## 고가용성을 위한 대표적인 방법

### 1. 서버 다중화
여러 Application Server를 운영하여 서버 한 대의 장애가 전체 장애로 이어지지 않게 함

### 2. Load Balancer
트래픽을 여러 서버로 분산하고 장애 서버를 트래픽 대상에서 제외

### 3. Multi-AZ
여러 AZ에 자원을 분산하여 AZ 단위 장애에 대응

### 4. Database Replication / Failover
DB를 복제하고 Primary 장애 시 다른 DB로 전환

### 5. Auto Scaling
필요한 서버 수를 유지하고 비정상 Instance가 제거되었을 때 새로운 Instance를 생성할 수 있음

### 6. Stateless 구조
특정 Application Server의 상태에 의존하지 않도록 함

예를 들어 Session을 서버 Memory에만 저장하면 서버 장애 시 상태를 잃을 수 있음

따라서
- Redis 기반 Session
- Token 기반 인증
등을 활용하여 특정 서버에 대한 의존성을 줄일 수 있음

## 비용과의 Trade-off
고가용성을 높일수록 일반적으로 비용과 운영 복잡성도 증가
```
EC2 1대
→ EC2 여러 대

DB 1대
→ Primary + Standby

Single-AZ
→ Multi-AZ
```
따라서 모든 시스템을 무조건 최고 수준의 HA로 구성하는 것이 아니라 **서비스가 요구하는 가용성 수준과 비용 사이에서 적절한 구조를 선택하는 것**이 중요

## 헷갈리는 포인트

### 서버를 여러 대 두면 무조건 HA인가?

아님. 모든 서버가 같은 AZ에 있거나 DB가 단일 구성이라면 여전히 SPOF가 존재할 수 있음

### Auto Scaling과 HA는 같은 개념인가?

아님. Auto Scaling은 서버 수를 조절하고 필요한 Instance 수를 유지하는 기능이고, HA를 구현하는 데 활용되는 수단 중 하나임

### Multi-AZ면 절대 장애가 발생하지 않는가?

아님. AZ 장애에는 대응할 수 있지만 애플리케이션 오류, Region 장애, 잘못된 배포 등 다른 원인의 장애는 여전히 발생할 수 있음

## 핵심 정리

### High Availability
- 장애가 발생해도 서비스를 계속 제공할 수 있도록 설계
- 목표는 **다운타임과 장애 영향 최소화**
- SPOF를 제거하는 것이 중요

### 대표적인 구성
- 여러 Application Server
- Load Balancer
- Multi-AZ
- DB Replication
- Failover
- Auto Scaling
- Stateless 설계

```
HA의 핵심

장애를 없앤다
(X)

장애가 발생해도
전체 서비스가 멈추지 않게 한다
(O)
```

> **고가용성은 서버를 여러 대 두는 기술 하나가 아니라, Application/Database/Network/AZ 등 전체 시스템에서 SPOF를 줄여가는 설계 방식**
