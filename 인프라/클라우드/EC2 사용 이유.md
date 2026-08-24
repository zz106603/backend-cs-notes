---
title: "EC2 사용 이유"
tags:
  - "Cloud"
  - "EC2"
  - "클라우드"
  - "서버"
---
# EC2 사용 이유

## 개요

과거에는 기업이 물리 서버를 직접 구매하고 데이터센터에 설치해 운영하는 **On-Premise 방식**이 일반적이었음

Cloud에서는 물리 서버를 직접 구매하지 않고 AWS가 제공하는 **EC2와 같은 컴퓨팅 자원**을 필요할 때 생성하여 사용할 수 있음

> 서버를 물리 장비가 아니라 필요할 때 생성/변경/삭제할 수 있는 자원으로 다룰 수 있게 됨

## 물리 서버의 한계

물리 서버는 CPU, Memory, Disk, Network 등의 하드웨어를 가진 실제 장비

기존에는 서버가 필요하면 다음 과정을 거쳐야 했음
```
서버 구매
→ 배송 및 설치
→ Network 설정
→ OS 설치
→ Application 배포
```

### 문제점

**1. 자원 활용률**

서비스에 필요한 최대 트래픽을 예상하여 서버를 구매하기 때문에 평소에는 CPU와 Memory가 남을 수 있음

**2. 느린 확장**

트래픽이 갑자기 증가해도 새로운 서버를 즉시 추가하기 어려움

**3. 높은 운영 부담**

하드웨어 장애, 교체, 설치 등을 직접 관리해야 함

**4. 초기 비용**

서비스를 시작하기 전에 서버와 네트워크 장비 등을 미리 구매해야 함

## 가상화(Virtualization)

**하나의 물리 서버의 자원을 나누어 여러 개의 독립적인 가상 서버를 실행하는 기술**
```
Physical Server
├─ VM 1
├─ VM 2
└─ VM 3
```
각 VM(Virtual Machine)은 독립적인 OS와 프로세스 환경을 가짐

### Hypervisor

VM을 생성하고 물리 자원을 분배/격리하는 계층

주요 역할
- CPU / Memory 등의 자원 할당
- VM 간 격리
- VM 생성 및 실행 관리

대표적으로 KVM, VMware 등이 있으면 AWS EC2는 현재 대부분 **Nitro System의 Nitro Hypervisor**를 기반으로 동작함

## EC2(Elastic Compute Cloud)

**AWS가 제공하는 컴퓨팅 서비스**

일반적으로 AWS의 물리 서버 위에서 가상화된 서버인 **EC2 Instance**를 생성하여 사용
```
AWS Physical Server
        ↓
Virtualization
        ↓
EC2 Instance
        ↓
Linux / Windows
        ↓
Application
```

EC2 안에서는 일반적인 서버처럼 필요한 프로그램을 실행할 수 있음

```
EC2
├─ Linux
├─ Java / Spring Boot
├─ Nginx
└─ Docker
```

> EC2의 중요한 점은 단순히 VM이라는 것보다 **서버를 몇 분 안에 생성/변경/삭제할 수 있다는 것**

## EC2 사용 이유

### 1. 빠른 서버 생성

물리 서버를 구매할 필요 없이 Console, CLI, API 등을 통해 빠르게 Instance를 생성할 수 있음

```
서버 필요
→ EC2 생성
→ Application 배포
```

테스트 환경이나 임시 서버도 쉽게 만들고 제거할 수 있음

### 2. 확장성이 좋음

트래픽이 증가하면 EC2를 추가하고 감소하면 제거할 수 있음

```
평상시
EC2 EC2

트래픽 증가
   ↓

EC2 EC2 EC2 EC2
```

Auto Scaling과 연결하면 이러한 확장과 축소를 자동화할 수도 있음

### 3. 초기 비용 감소

서버를 미리 구매하지 않고 사용하는 컴퓨팅 자원에 대해 비용을 지불할 수 있음

따라서 큰 초기 인프라 투자 없이 서비스를 시작하기 쉬움

다만 **EC2가 항상 물리 서버보다 저렴하다는 의미는 아님.** 장기간 과도한 자원을 사용하거나 불필요한 Instance를 방치하면 비용이 커질 수 있음

### 4. 운영 자동화

EC2는 API를 통해 관리할 수 있기 때문에 인프라 자동화와 잘 결합됨

예를 들어
- Auto Scaling
- CI/CD를 통한 자동 배포
- Terraform 등을 이용한 IaC
- Snapshot / Image를 이용한 서버 복구 및 재생성

즉, 서버를 **소프트웨어처럼 자동화하여 관리할 수 있음**

### 5. 글로벌 인프라 활용

필요한 AWS Region을 선택하여 EC2를 생성할 수 있음

```
한국 서비스 → 서울 Region
일본 서비스 → 도쿄 Region
미국 서비스 → 미국 Region
```

직접 각 국가에 데이터센터를 구축하는 것보다 훨씬 쉽게 글로벌 인프라를 사용할 수 있음

## EC2 Instance Type

EC2는 필요한 CPU, Memory 등의 특성에 따라 여러 Instance Family를 제공

| 계열   | 특징        |
| ---- | --------- |
| T 계열 | 범용 / 버스터블 |
| M 계열 | 범용        |
| C 계열 | CPU 중심    |
| R 계열 | Memory 중심 |

예를 들어 CPU 연산이 많은 서비스와 Memory를 많이 사용하는 서비스가 같은 종류의 서버를 사용할 필요가 없음

필요한 워크로드에 맞는 Instance Type을 선택하는 것이 성능과 비용 측면에서 중요함

## EC2 직접 관리

EC2를 사용한다고 서버 운영이 모두 AWS 책임이 되는 것은 아님

EC2는 **IaaS(Infrastructure as a Service)** 이기 때문에 일반적으로 사용자가 다음 영역을 관리해야 함
- OS
- Application
- 보안 설정
- OS 및 Software Patch
- 배포
- 장애 대응

```
물리 서버 / Data Center
→ AWS 관리

EC2 내부 OS / Application
→ 사용자 관리
```

## EC2 내부 설치

기술적으로 EC2에 다음과 같은 것들을 직접 설치 가능
```
EC2
├─ Spring Boot
├─ MySQL
├─ Redis
├─ RabbitMQ
└─ Nginx
```

하지만 DB나 Cache까지 직접 운영하면 장애 대응, Backup, Patch 등의 운영 부담이 커짐

그래서 AWS에서는 관리형 서비스를 함께 사용하는 경우가 많음

| 역할                 | AWS 서비스 예시  |
| ------------------ | ----------- |
| Application Server | EC2         |
| Database           | RDS         |
| Object Storage     | S3          |
| Cache              | ElastiCache |

즉, **직접 제어할 필요가 있는 영역은 EC2를 사용하고 운영 부담을 줄이고 싶은 영역은 Managed Service를 활용**할 수 있음

## 물리 서버 vs EC2

| 항목      | 물리 서버     | EC2           |
| ------- | --------- | ------------- |
| 서버 생성   | 구매·설치 필요  | 빠르게 생성 가능     |
| 확장      | 느림        | 빠름            |
| 초기 비용   | 높음        | 상대적으로 낮음      |
| 자동화     | 상대적으로 어려움 | API 기반 자동화 용이 |
| 글로벌 배포  | 직접 구축 필요  | Region 선택 가능  |
| 하드웨어 관리 | 직접 관리     | AWS 관리        |

## 헷갈리는 포인트

### EC2는 실제 서버가 없는 것인가?

아님. EC2 역시 결국 AWS 데이터센터의 실제 물리 서버 위에서 동작함

### EC2와 Docker Container는 같은가?

아님. 일반적인 EC2 Instance는 **VM 기반의 서버 환경**이고 Container는 OS Kernel을 공유하는 더 가벼운 실행 단위

```
Physical Server
    ↓
EC2 VM
    ↓
Linux
    ↓
Docker
    ↓
Container
```

EC2 위에서 Docker Container를 실행할 수도 있음

### EC2를 사용하면 운영이 필요 없는가?

아님. EC2는 IaaS이므로 OS, 보안, Patch, Application 등은 여전히 사용자가 관리해야 함

운영 부담 자체를 크게 줄이고 싶다면 RDS, Lambda 등의 Managed Service를 고려할 수 있음

## 핵심 정리

### EC2
- AWS가 제공하는 컴퓨팅 서비스
- 일반적으로 가상화된 서버인 EC2 Instance를 사용
- 필요할 때 빠르게 생성/변경/삭제 가능
- 일반 Linux 서버처럼 Application 실행 가능

### 물리 서버 대신 EC2를 사용하는 이유
- 빠른 서버 생성
- 쉬운 확장
- 초기 투자 부담 감소
- 자동화 용이
- 글로벌 인프라 활용

```
물리 서버
= 장비를 구매해서 운영

EC2
= 필요한 서버 자원을
  필요할 때 생성해서 사용
```

> **EC2의 핵심은 단순한 "가상 서버"가 아니라 서버 인프라를 필요에 따라 빠르게 생성하고 자동화할 수 있다는 점**
