---
title: "Elasticity (탄력성)"
tags:
  - "탄력성"
  - "Elasticity"
  - "Cloud"
---
# Elasticity (탄력성)

## 개요

**Elasticity(탄력성)** 는 Cloud Computing의 핵심 특징 중 하나

서비스의 트래픽은 항상 일정하지 않기 때문에 부하에 따라 자원을 늘리거나 줄일 수 있어야 함

> **Elasticity = 필요한 시점에 자원을 늘리고, 필요하지 않을 때 다시 줄이는 능력**

## Elasticity

**시스템의 부하 변화에 따라 컴퓨팅 자원을 빠르게 또는 자동으로 확장/축소할 수 있는 능력**

```
평상시
EC2  EC2

   ↓ 트래픽 증가

EC2  EC2  EC2  EC2  EC2

   ↓ 트래픽 감소

EC2  EC2
```

## 필요 이유
실제 서비스의 트래픽은 계속 변함

- 쇼핑몰 할인 이벤트
- 게임 업데이트
- 특정 시간대의 사용자 집중
- 뉴스나 이슈로 인한 접속 증가

On-Premise에서는 최대 트래픽을 예상하여 서버를 미리 구매해야 했음
```
최대 필요 서버: 100대

평상시 실제 필요: 20대
→ 나머지 서버 자원 낭비
```
Cloud에서는 필요할 때 서버를 추가하고 트래픽이 감소하면 제거할 수 있으므로 자원 활용을 효율적으로 할 수 있음

## Scalability vs Elasticity

### Scalability
**더 많은 부하를 처리하도록 시스템을 확장할 수 있는 능력**

- Scale Up
- Scale Out

### Elasticity
**현재 부하에 맞게 자원을 늘렸다가 다시 줄이는 능력**

| 개념          | 의미                             |
| ----------- | ------------------------------ |
| Scalability | 더 큰 부하를 처리할 수 있도록 확장 가능한 능력    |
| Elasticity  | 부하 변화에 따라 자원을 유동적으로 확장·축소하는 능력 |

## AWS에서 Elasticity
Cloud는 물리 서버와 달리 EC2 Instance를 빠르게 생성하고 제거할 수 있기 때문에 Elasticity를 구현하기 쉬움

AWS에서는 대표적으로 다음 서비스를 활용
- **Amazon EC2 Auto Scaling**
- **Elastic Load Balancing**
```
             ┌─ EC2
Client → ALB ├─ EC2
             └─ EC2
                 ↑
            Auto Scaling
```
트래픽이 증가하면 Auto Scaling이 EC2를 추가하고, Load Balancer가 새로 추가된 서버에도 요청을 분산

## Auto Scaling
**Auto Scaling은 설정한 조건에 따라 EC2 Instance 수를 자동으로 조절하는 기능**

```
CPU 사용률 증가
     ↓
EC2 추가

CPU 사용률 감소
     ↓
EC2 감소
```
실제 정책은 단순 CPU 기준뿐만 아니라 다양한 CloudWatch Metric 등을 이용할 수 있음

Elasticity는 개념이고, **Auto Scaling은 이를 구현하기 위한 대표적인 방법 중 하나**

## Elasticity의 장점

### 1. 비용 효율
트래픽이 적을 때 불필요한 서버를 줄일 수 있음

### 2. 트래픽 변화 대응
갑작스러운 부하 증가에 필요한 자원을 추가할 수 있음

### 3. 운영 자동화
관리자가 직접 서버를 생성하고 제거하는 작업을 줄일 수 있음

### 4. 자원 활용 효율
항상 최대 트래픽 기준의 서버를 유지하지 않아도 됨

## Elasticity와 Stateless
Elasticity는 주로 **Scale Out**과 함께 사용됨

서버가 계속 생성되고 제거될 수 있기 때문에 특정 서버에 상태가 저장되어 있으면 문제가 발생할 수 있음
```
EC2-1
└─ 사용자 Session 저장

EC2-1 제거
→ Session도 사라질 수 있음
```

따라서 Application Server를 가능한 한 Stateless하게 구성하고, 필요한 상태는 Redis나 Database 같은 외부 저장소에서 관리하는 것이 유리함
```
EC2-1 ─┐
EC2-2 ─┼→ Redis / DB
EC2-3 ─┘
```

## Elasticity의 한계
Elasticity라고 해서 자원이 즉시 무한하게 증가하는 것은 아님

### 1. 확장 시간
EC2를 새로 생성하고 Application을 실행하기까지 시간이 필요함

### 2. 다른 시스템의 병목
Application Server를 늘려도 Database가 처리하지 못하면 전체 성능은 증가하지 않음
```
EC2 × 10
   ↓
DB 1대 ← 새로운 병목
```

### 3. 비용 증가
Auto Scaling 정책을 잘못 설정하면 예상보다 많은 Instance가 생성되어 비용이 증가할 수 있음

### 4. Scale Out 가능한 구조 필요
Session이나 Local File 등 특정 서버의 상태에 강하게 의존하면 서버를 자유롭게 생성/삭제하기 어려움

## Container와 Elasticity
Elasticity는 Container 환경에서도 중요함

Kubernetes에서는 트래픽이나 부하에 따라 Pod 수를 자동으로 조절할 수 있음
```
트래픽 증가
→ Pod 증가

트래픽 감소
→ Pod 감소
```

EC2 Auto Scaling이 **서버 단위 확장**이라면 Kubernetes에서는 **Container/Pod 단위 확장**도 가능하다고 이해하면 됨

## 핵심 정리

### Elasticity
- 부하 변화에 따라 자원을 확장하거나 축소하는 능력
- Cloud의 빠른 자원 생성/삭제 특성과 잘 맞음

### AWS에서의 대표 구성
- EC2
- Auto Scaling
- Elastic Load Balancing

### Scalability와 차이
```
Scalability
= 더 큰 부하를 처리할 수 있도록 확장 가능한가?

Elasticity
= 실제 부하에 맞춰 자원을 늘리고 다시 줄일 수 있는가?
```

> **Elasticity의 핵심은 단순히 서버를 늘리는 것이 아니라, 필요한 만큼 늘리고 필요가 없어지면 다시 줄이는 것**
