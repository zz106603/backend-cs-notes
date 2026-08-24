---
title: "Managed Service"
tags:
  - "Cloud"
  - "AWS"
---
# Managed Service

## 개요

Cloud에서는 모든 시스템을 직접 설치하고 운영할 필요가 없음

일부 인프라 운영을 AWS 같은 Cloud Provider에게 맡길 수 있는데, 이를 **Managed Service**라고 함

> 핵심은 **직접 운영해야 하는 영역을 줄이고 애플리케이션과 비즈니스 로직에 더 집중하는 것**

## Managed Service
**서버나 소프트웨어의 설치, 패치, 백업, 장애 대응 등 운영 작업의 일부를 Cloud Provider가 대신 관리해주는 서비스**

예를 들어 MySQL을 운영

### EC2에 직접 설치
```
EC2
 ↓
MySQL 직접 설치

사용자
├─ DB 설치
├─ OS 관리
├─ Backup
├─ Replication
├─ 장애 대응
└─ Failover 구성
```

### Amazon RDS 사용
```
Amazon RDS
     ↓
AWS가 인프라와 DB 운영 일부 관리

사용자
├─ Schema 설계
├─ SQL 작성
├─ Query 최적화
└─ Application 연동
```
RDS를 사용하면 DB 서버 자체를 직접 구축하는 부담을 상당 부분 줄일 수 있음

## 대표적인 AWS Managed Service

| 역할             | 서비스                |
| -------------- | ------------------ |
| Database       | Amazon RDS         |
| Object Storage | Amazon S3          |
| Cache          | Amazon ElastiCache |
| Message Queue  | Amazon SQS         |
| Container      | Amazon ECS         |
| Kubernetes     | Amazon EKS         |

서비스마다 **AWS가 대신 관리해주는 범위는 다름**

예를 들어 EKS는 Kubernetes Control Plane을 AWS가 관리하지만, 구성 방식에 따라 Worker Node 운영은 사용자가 담당할 수도 있음

## Managed Service 사용 이유
실제 시스템 운영에서는 단순히 프로그램을 설치하는 것보다 이후 관리가 더 어려움

예를 들어 Redis를 EC2에 직접 설치하면 다음을 고려해야 함
- 장애 복구
- Replication
- Failover
- Backup / Persistence
- Memory 관리
- Cluster 구성
- Patch

ElastiCache를 사용하면 이러한 운영 영역의 상당 부분을 AWS가 대신 관리함

## Shared Responsibility Model
Managed Service를 사용한다고 모든 책임이 AWS로 넘어가는 것은 아님

AWS와 사용자가 책임을 나누어 가짐

### EC2
```
AWS
├─ Data Center
├─ Physical Server
└─ 기본 Infrastructure

사용자
├─ OS
├─ OS Patch
├─ Application
└─ Application 설정 및 데이터
```

### RDS
```
AWS
├─ Physical Infrastructure
├─ OS
├─ DB Software 설치
├─ 자동 Backup 기능
└─ Multi-AZ 구성 시 Failover 지원

사용자
├─ Schema
├─ Data
├─ SQL
├─ Index
├─ Parameter 설정
└─ Query 성능 관리
```

**Managed Service를 사용할수록 운영 책임은 줄어들지만, 애플리케이션과 데이터에 대한 책임까지 없어지는 것은 아님**

## Managed Service의 장점

### 1. 운영 부담 감소
설치, Backup, Failover, Patch 등의 작업을 직접 관리해야 하는 범위를 줄일 수 있음

### 2. 빠른 구축
직접 서버를 설치하고 구성하는 것보다 빠르게 필요한 서비스를 사용할 수 있음

### 3. 장애 대응 기능 활용
서비스에 따라 다음과 같은 기능을 제공함
- Multi-AZ
- 자동 Backup
- Snapshot
- Failover
- Monitoring

### 4. 적은 운영 인력으로 관리 가능
DB나 Cache 전문 운영 인력이 부족한 팀에서도 비교적 안정적인 인프라를 구축하기 쉬움

## Managed Service의 단점

### 1. 제어권 감소
RDS를 사용하면 일반적인 EC2처럼 OS에 직접 접근하거나 모든 내부 설정을 자유롭게 변경할 수 없음

### 2. 비용 증가 가능
직접 구축하는 것보다 Managed Service 사용 비용이 더 높을 수 있음

### 3. Vendor Lock-in
AWS 고유 기능에 많이 의존하면 다른 Cloud Provider로 이전하기 어려워질 수 있음

## 운영 부담과 제어권이 Trade-off
Managed Service를 선택할 때 핵심은 **운영 부담과 제어권 사이의 균형**
```
직접 운영

높은 제어권
↑
운영 부담도 큼


Managed Service

낮은 운영 부담
↑
일부 제어권 감소
```

무조건 Managed Service가 좋은 것도 아니고, 무조건 직접 운영하는 것이 좋은 것도 아님

서비스 규모, 운영 인력, 비용, 필요한 제어 수준에 따라 선택

## 내부 원리
RDS를 사용한다고 Database에 대한 지식이 필요 없어지는 것은 아님

백엔드 개발자는 여전히 다음을 이해해야 함
- Connection Pool
- Index
- Slow Query
- Transaction / Lock
- Replication
- Failover

마찬가지로 ElastiCache를 사용해도 Cache 동작 방식과 장애 상황을 이해해야 함

**Managed Service는 기술을 몰라도 되게 만드는 것이 아니라, 직접 운영해야 하는 범위를 줄여주는 것**

## 헷갈리는 포인트

### Managed Service면 AWS가 전부 관리하는가?

아님. AWS가 관리하는 범위는 서비스마다 다르며 데이터, 애플리케이션, 설정 등은 여전히 사용자의 책임

### RDS를 쓰면 DB 성능 문제도 AWS가 해결해주는가?

아님. Slow Query, 잘못된 Index, Connection Pool 문제 등은 사용자가 분석하고 개선해야 함

### Managed Service가 항상 더 저렴한가?

아님. 운영 편의성을 얻는 대신 직접 운영보다 비용이 높을 수도 있음

## 핵심 정리

### Managed Service
- Cloud Provider가 인프라 운영의 일부를 대신 담당
- 개발자의 운영 부담을 줄이는 것이 목적

### 장점
- 운영 부담 감소
- 빠른 구축
- Backup / Failover 등 관리 기능 활용
- 개발에 집중하기 쉬움

### 단점
- 제어권 감소
- 비용 증가 가능
- Vendor Lock-in

```
직접 운영
= 자유도 높음 + 운영 부담 높음

Managed Service
= 운영 부담 낮음 + 일부 제어권 포기
```

> **Managed Service의 핵심은 "운영을 안하는 것"이 아니라, 운영 책임의 일부를 Cloud Provider에게 맡기는 것**
