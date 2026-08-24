---
title: "Public Cloud"
tags:
  - "Cloud"
  - "클라우드"
  - "Public"
  - "Private"
  - "Hybrid"
---
# Public Cloud

## 개요

Cloud는 인프라를 누가 소유하고 누구에게 제공하는지에 따라 대표적으로 다음과 같이 구분

- **Public Cloud**
- **Private Cloud**
- **Hybrid Cloud**

## Public Cloud

**클라우드 사업자가 구축한 인프라를 여러 고객에게 서비스 형태로 제공하는 방식**

대표적 Public Cloud
- AWS
- Microsoft Azure
- Google Cloud Platform(GCP)

사용자는 서버나 데이터센터를 직접 구매하지 않고 필요한 컴퓨팅, 스토리지, 데이터베이스 등의 자원을 필요한 만큼 사용
```
Cloud Provider 
        ↓ 
대규모 인프라 운영 
        ↓ 
┌────┼────┐ 
고객 A  고객 B   고객 C
```
> **Public Cloud의 핵심은 직접 인프라를 소유하지 않고 필요한 자원을 서비스 형태로 사용하는 것**

## 주요 특징
| 특징              | 설명                       |
| --------------- | ------------------------ |
| 클라우드 사업자 소유     | AWS 등의 사업자가 물리 인프라 운영    |
| Multi-Tenant    | 여러 고객이 공유 인프라 사용         |
| 사용량 기반 과금       | 사용한 자원에 따라 비용 지불         |
| 빠른 확장           | 필요한 자원을 빠르게 생성·확장 가능     |
| 글로벌 인프라         | 여러 Region을 선택하여 사용 가능    |
| Managed Service | DB, Storage 등 관리형 서비스 제공 |

### Multi-Tenant

Public Cloud는 하나의 클라우드 인프라를 여러 고객이 함께 사용하는 **Multi-Tenant 구조**를 기본으로 함

하지만 가상화와 접근 제어 등을 통해 고객별 환경은 서로 격리됨
```
Physical Infrastructure
        ↓
   가상화 / 격리
        ↓
   ├─ 고객 A
   ├─ 고객 B
   └─ 고객 C
```
즉, 인프라를 공유한다고 해서 다른 고객의 서버나 데이터에 접근할 수 있는 것은 아님

## Public Cloud 사용 이유
기존 On-Premise 환경에서는 서버가 필요하면 직접 구매하고 설치해야 했음
```
서버 구매
→ 설치
→ 네트워크 설정
→ OS 설정
→ 서비스 배포
```
Public Cloud에서는 이미 구축된 인프라에서 필요한 자원을 빠르게 생성할 수 있음
```
서버 필요
→ EC2 생성
→ 사용
```
즉, **초기 인프라 투자 부담을 줄이고 필요한 시점에 자원을 빠르게 확보할 수 있음**

---

### 장점
**1. 초기 비용 감소**
   
서버나 데이터센터를 직접 구축할 필요가 없음

**2. 빠른 확장**

EC2 등의 자원을 빠르게 추가할 수 있고 Auto Scaling을 통해 트래픽 증가에도 대응할 수 있음

**3. 글로벌 인프라**

서울, 도쿄, 미국 등 여러 Region을 선택하여 서비스를 구축할 수 있음

**4. Managed Service 활용**

AWS가 관리하는 서비스를 활용하여 직접 운영해야 하는 범위를 줄일 수 있음
- RDS
- S3
- SQS
- Lambda

---

### 단점
**1. Vendor Lock-in**

AWS 전용 서비스에 많이 의존하면 다른 Cloud Provider로 이전하기 어려워질 수 있음

**2. 비용 관리**

초기 비용은 적지만 사용량이 증가하거나 불필요한 자원을 방치하면 비용이 크게 증가할 수 있음
- 미사용 EC2
- 데이터 전송 비용
- 과도한 Auto Scaling
- 불필요한 Storage

**3. Cloud Provider 의존**

물리 인프라를 직접 관리하지 않기 때문에 Cloud Provider의 장애나 정책에도 영향을 받을 수 있음

**4. 규제 고려**

금융, 의료, 공공 등의 시스템에서는 데이터 저장 위치나 보안 규제를 추가로 고려해야 할 수 있음

## Private Cloud

**특정 조직만을 위해 구성된 전용 Cloud 환경**

특징
- 특정 조직 전용
- 인프라 통제력이 높음
- 조직 정책에 맞는 구성 가능
- 직접 구축/운영해야 하는 영역이 많음
- 비용과 운영 부담이 커질 수 있음

OpenStack이나 VMware 등을 이용해 구축하기도 함

> 단순히 회사 내부에 서버가 있다고 Private Cloud인 것은 아님. 가상화, 자동화, API 기반 자원 관리 등 Cloud의 특징을 제공해야 함

 ## Hybrid Cloud

 **Public Cloud와 Private Cloud 또는 기존 On-Premise 환경을 함께 사용하는 구조**
 ```
On-Premise / Private Cloud
           │
           │ 연결
           ↓
     Public Cloud
```

예를 들어 다음과 같이 구성할 수 있음
```
민감한 핵심 시스템
→ Private / On-Premise

일반 서비스
→ AWS Public Cloud
```
주로 다음과 같은 이유로 사용됨
- 기존 시스템 유지
- 보안 및 규제
- 단계적인 Cloud 전환

## Public Cloud vs Private Cloud
| 구분     | Public Cloud   | Private Cloud |
| ------ | -------------- | ------------- |
| 제공 대상  | 여러 고객          | 특정 조직         |
| 인프라 운영 | Cloud Provider | 기업 또는 전용 사업자  |
| 자원 구조  | 공유 인프라 기반      | 조직 전용         |
| 초기 비용  | 상대적으로 낮음       | 상대적으로 높음      |
| 확장성    | 높음             | 보유 자원에 영향     |
| 통제력    | 상대적으로 낮음       | 높음            |
| 운영 부담  | 상대적으로 낮음       | 높음            |

## 헷갈리는 포인트

### Public Cloud는 서버가 인터넷에 공개된다는 의미?

아님. **Public은 클라우드 서비스를 여러 고객에게 제공한다는 의미**이며 서버의 인터넷 공개 여부와는 별개임

### Public Cloud는 보안이 약한가?

아님. Cloud Provider가 물리 인프라와 기본 보안을 제공하고, 사용자는 자신의 계정/네트워크/애플리케이션을 올바르게 설정해야 함

즉, Public Cloud 자체가 보안이 약한 것이 아니라 **어떻게 설계하고 운영하느냐가 중요**

### Public Cloud가 항상 더 저렴한가?

아님. 초기 구축 비용은 줄일 수 있지만 장기간 많은 자원을 사용하거나 비용 관리를 하지 않으면 오히려 비용이 커질 수 있음

Public Cloud의 핵심 장점은 단순한 **저렴함**보다는 **필요한 자원을 빠르고 유연하게 사용할 수 있다는 것**에 가까움

## 핵심 정리

### Public Cloud
- AWS, Azure, GCP 등이 대표적
- Cloud Provider가 인프라를 구축하고 운영
- 여러 고객이 공유 인프라 사용
- 필요한 만큼 자원을 사용하고 비용 지불

### Private Cloud
- 특정 조직 전용 Cloud
- 높은 통제력
- 구축/운영 부담 증가

### Hybrid Cloud
- Public Cloud와 Private Cloud 또는 On-Premise를 함께 사용
- 기존 시스템, 규제, 단계적 Cloud 전환 등의 이유로 활용

```
Public Cloud
= 다른 회사와 서버를 공개해서 함께 쓰는 것
(X)

Public Cloud
= Cloud Provider가 운영하는 인프라를
  여러 고객이 격리된 환경에서 서비스 형태로 사용하는 것
(O)
```
