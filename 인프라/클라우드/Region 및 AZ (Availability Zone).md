---
title: "Region 및 AZ(Availability Zone)"
tags:
  - "클라우드"
  - "cloud"
  - "Region"
  - "AZ"
---
# Region 및 AZ(Availability Zone)

## Region 및 AZ(Availability Zone)

### 개요

AWS는 전 세계의 서버를 하나의 거대한 데이터센터에서 운영하지 않음

대신 인프라를 크게 **Region -> Availability Zone(AZ)** 구조로 나누어 운영

```
AWS
 ├─ Region: 서울
 │   ├─ AZ A
 │   ├─ AZ B
 │   └─ AZ C
 │
 ├─ Region: 도쿄
 │   ├─ AZ A
 │   ├─ AZ B
 │   └─ AZ C
 │
 └─ Region: 미국
     ├─ AZ A
     ├─ AZ B
     └─ AZ C
```
Region은 **지리적인 분리**, AZ는 **Region 내부의 장애 격리**


## Region
### AWS가 서비스를 제공하는 독립적인 지리적 영역

| Region Code | 위치 |
| --- | --- |
| ap-northeast-2 | 서울 |
| ap-northeast-1 | 도쿄 |
| us-east-1 | 미국 버지니아 |
| eu-west-1 | 아일랜드 |

각 Region은 여러 개의 Availability Zone으로 구성됨

AWS는 Region을 서로 물리적으로 분리하여 하나의 Region에서 발생한 장애가 다른 Region까지 직접 확한되지 않도록 설계

### Region을 나누는 이유

**1. Latency 감소**

사용자와 서버의 물리적 거리가 멀어질수록 네트워크 지연 시간이 증가

```
한국 사용자
    ↓
서울 Region  → 비교적 낮은 Latency

한국 사용자
    ↓
미국 Region  → 상대적으로 높은 Latency
```
서비스를 제공하는 주요 사용자와 가까운 Region을 선택하는 것이 중요함

**2. 장애 격리**

전 세계의 AWS 인프라가 하나의 영역으로 구성되어 있다면 대규모 장애가 전체 시스템에 영향을 줄 수 있음

Region을 물리적으로 분리하면 특정 Region에 큰 장애가 발생하더라도 다른 Region에서 운영되는 시스템에는 직접적인 영향을 줄일 수 있음

```
서울 Region 장애
      ↓
서울 서비스 영향

도쿄 Region
      ↓
독립적으로 운영 가능
```

**3. 법률 및 데이터 규제**

국가나 산업에 따라 특정 데이터를 해당 국가 또는 지역 안에 저장해야 하는 Data Residency 요구사항이 존재할 수 있음

특히 금융, 의료, 공공 서비스처럼 데이터 규제가 중요한 시스템에서는 Region 선택이 시스템 설계의 중요한 요소가 됨

## Availability Zone(AZ)

### AZ은 하나의 Region 내부에 존재하는 물리적으로 분리된 장애 격리 영역

> AZ = 데이터 센터 1개라고 단순하게 이해하면 안 됨

AWS 공식 정의에서 하나의 AZ는 **하나 이상의 독립된 데이터센터**로 구성될 수 있음

각 AZ는 다른 AZ와 전력, 냉각, 네트워크 등의 장애가 최대한 공유되지 않도록 물리적으로 분리되어 있음

```
서울 Region
│
├─ AZ A
│   └─ 하나 이상의 물리적 Data Center
│
├─ AZ B
│   └─ 하나 이상의 물리적 Data Center
│
└─ AZ C
    └─ 하나 이상의 물리적 Data Center
```

AZ들은 장애가 동시에 발생할 가능성을 줄이기 위해 의미 있는 물리적 거리를 두고 배치되지만,

동시에 서로 빠르게 통신할 수 있도록 **고대역폭/저지연 전용 네트워크**로 연결됨

AWS는 AZ 간 거리를 최대 약 100km 범위로 설명하며 동기식 데이터 복제가 가능한 수준의 낮은 지연 시간을 제공하도록 설계

## 왜 여러 AZ가 필요한가?

### 가장 중요한 목적은 High Availability(고가용성)

하나의 AZ에만 모든 서버를 배치하면 해당 AZ에 장애가 발생했을 때 서비스 전체가 중단될 수 있음

- 전원 장애
- 네트워크 장애
- 냉각 시스템 문제
- 화재
- 자연 재해
- 기타 데이터센터 장애

**Single-AZ**
```
사용자
  ↓
 EC2
 AZ A
```

**Multi-AZ**
```
              ┌─ EC2
사용자 → ALB ─┤  AZ A
              │
              └─ EC2
                  AZ B
```

AZ A에 장애가 발생하더라도 AZ B의 서버가 정상이라면 서비스를 계속 제공할 수 있음

실무에서는 **"하나의 AZ 전체가 장애가 나도 서비스가 살아있는가?"** 를 중요하게 봄

## AWS 서비스와 Region / AZ

Region과 AZ는 AWS의 여러 서비스와 직접 연결됨

### EC2
EC2 인스턴스는 특정 Region 안의 **특정 AZ에 위치**

실제로는 EC2를 생성할 때 선택하는 Subnet이 특정 AZ에 속하기 때문에, 어떤 Subnet에 EC2를 생성하는지가 EC2가 위치할 AZ를 결정함
```
VPC - 서울 Region
│
├─ Subnet A - AZ A
│   └─ EC2
│
└─ Subnet B - AZ B
    └─ EC2
```
> **VPC는 Region 범위이고, Subnet은 AZ 범위**

### RDS
Amazon RDS는 **Multi-AZ 배포**를 지원

대표적인 Multi-AZ DB Instance 구조에서는 Primary DB와 다른 AZ에 Standby DB를 배치
```
AZ A
Primary DB
    │
    │ 동기 복제
    ↓
AZ B
Standby DB
```
Primary DB나 해당 AZ에 장애가 발생하면 RDS가 Standby DB로 Failover할 수 있음

다만 Multi-AZ의 목적은 기본적으로 **고가용성**이지 단순한 조회 성능 향상이 아님

특히 일반적인 RDS Multi-AZ DB Instance의 Standby는 평상시 Read Replica처럼 조회 트래픽을 처리하기 위한 서버가 아님

### ELB
Elastic Load Balancing은 여러 AZ에 존재하는 서버로 요청을 분산할 수 있음
```
               ┌─ EC2 - AZ A
Client → ALB ──┤
               └─ EC2 - AZ B
```
이를 통해 특정 EC2뿐만 아니라 특정 AZ에 장애가 발생한 상황에도 다른 AZ의 인스턴스를 이용하여 서비스를 제공하도록 설계할 수 있음

## Multi-AZ와 Multi-Region

### Multi-AZ
```
서울 Region
│
├─ AZ A → Server
└─ AZ B → Server
```
같은 Region 안에서 여러 AZ를 사용

주요 목적은 **고가용성과 AZ 장애 대응**

### Multi-Region
```
서울 Region
   ↓
Service
   +
도쿄 Region
   ↓
Service
```
서로 다른 Region에 서비스를 구축

Region 전체 장애에도 대응할 수 있지만 그만큼 시스템 복잡도가 크게 증가함

예를 들어 다음 문제들을 추가로 고려해야 함
- Region 간 데이터 복제
- 데이터 정합성
- DNS 및 트래픽 라우팅
- Failover 정책
- 네트워크 지연
- 운영 비용
- 배포 및 모니터링

따라서 일반적으로는 아래 관점으로 접근함
```
Multi-AZ
→ 일반적인 Production 고가용성

Multi-Region
→ Region 장애 대응 / Disaster Recovery / 글로벌 서비스
```
Multi-Region이 무조건 더 좋은 구조인 것이 아니라 **요구되는 가용성과 비용, 복잡성 사이의 선택**임

## Region vs AZ
| 구분    | Region               | Availability Zone   |
| ----- | -------------------- | ------------------- |
| 의미    | 독립적인 지리적 영역          | Region 내부의 장애 격리 영역 |
| 범위    | 국가/지역 수준             | Region 내부 물리적 위치    |
| 구성    | 여러 AZ                | 하나 이상의 데이터센터        |
| 주요 목적 | 지역 분리, Latency, 규제   | 고가용성, 장애 격리         |
| 네트워크  | Region 간 상대적으로 높은 지연 | 저지연·고대역폭 연결         |
| 대표 설계 | Multi-Region         | Multi-AZ            |

## 헷갈리기 쉬운 부분

### 서울 Region은 데이터센터 하나인가?

아님. 하나의 Region에는 여러 AZ가 존재하며 각 AZ 역시 하나 이상의 물리적 데이터센터로 구성될 수 있음
```
Region
  ↓
여러 AZ
  ↓
각 AZ는 하나 이상의 Data Center
```

### AZ는 단순한 논리적 구분인가?

아님. 전력, 냉각, 네트워크 등 장애가 서로 영향을 주지 않도록 물리적으로 분리된 **Fault Isolation Boundary(장애 격리 경계)**

### AZ끼리는 멀리 떨어져 있으니 통신이 느린가?

아님. 장애 격리를 위해 물리적으로 떨어져 있지만 AWS 전용 저지연/고대역폭 네트워크로 연결되어 있음

그래서 AZ 간 DB 동기 복제나 Load Balancing 같은 구조를 사용할 수 있음

### Multi-AZ면 무조건 무중단인가?

아님. Multi-AZ는 **장애 발생 시 서비스를 계속 제공할 가능성을 높이는 구조**

Failover 과정에서 짧은 중단이 발생할 수 있고 애플리케이션에서도 다음을 고려해야 함
- DB Connection 재연결
- Timeout / Retry
- 사용자 Session 저장 위치
- 상태를 가진 서버 처리
- Health Check

즉, **인프라를 Multi-AZ로 만들었다고 애플리케이션까지 자동으로 고가용성이 되는 것은 아님**

### Multi-Region이 Multi-AZ보다 항상 좋은가?

아님. 

Multi-Region은 Region 전체 장애까지 대응할 수 있지만 데이터 복제와 정합성, 네트워크, DNS, Failover 등 고려해야 할 사항과 비용이 크게 증가함

대부분의 일반적인 시스템에서는 먼저 Multi-AZ를 통해 고가용성을 확보하고, 정말 필요한 경우 Multi-Region을 고려함

## 핵심 정리

### Region?
AWS 인프라가 위치하는 독립적인 **지리적 영역**

Region 선택
- 사용자와의 거리와 Latency
- 장애 격리
- 데이터 저장 위치와 규제

### Availability Zone?
Region 내부의 **물리적으로 분리된 장애 격리 영역**

하나의 AZ는 하나 이상의 데이터센터로 구성될 수 있으며 다른 AZ와 독립적인 전력, 네트워크, 냉각 시스템 등을 사용하도록 설계

### 왜 Multi-AZ를 사용?
하나의 서버뿐만 아니라 **AZ 전체 장애에도 서비스를 유지하기 위해서**
```
Region = 어디에 서비스를 둘 것인가?
AZ     = 한 데이터센터 영역이 죽어도 버틸 수 있는가?
```
