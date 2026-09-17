---
title: "Kubernetes 기본 구조"
tags:
  - "쿠버네티스"
  - "Docker"
  - "Kubernetes"
---
# Kubernetes 기본 구조

## Kubernetes

> 여러 서버에서 실행되는 컨테이너를 **배포하고, 상태를 유지하고, 확장하고, 관리하기 위한 Container Orchestration Platform**

Docker만으로도 Spring Boot 애플리케이션을 컨테이너로 실행할 수 있음
```Bash
docker run -d -p 8080:8080 my-api
```
하지만 실제 운영 환경에서는 애플리케이션을 하나만 실행하는 것이 아니라 여러 개의 인스턴스를 운영하게 됨

- Container 하나가 죽으면 다시 실행해야 함
- 트래픽이 증가하면 Container 수를 늘려야 함
- 여러 Container로 요청을 분산해야 함
- 새로운 버전을 서비스 중단 없이 배포해야 함
- 각 애플리케이션의 설정값과 환경 변수를 관리해야 함

**Kubernetes는 이러한 컨테이너 운영을 자동화**

## Kubernetes의 핵심 개념: Desired State

> **원하는 상태를 선언하는 것**

ex) Spring Boot 애플리케이션을 항상 3개 실행

```
Desired State = 3
Current State = 3

하나 종료

Desired State = 3
Current State = 2
```

Kubernetes는 이 차이를 감지하고 새로운 애플리케이션을 하나 실행하여 다시 3개를 유지
```
Pod 3개 실행
    ↓
Pod 하나 장애
    ↓
Pod 2개
    ↓
Kubernetes가 새로운 Pod 생성
    ↓
Pod 3개
```
> Kubernetes는 단순히 Container를 실행하는 도구가 아니라 **선언된 상태와 실제 상태를 계속 일치시키는 시스템**

## Docker와 Kubernetes 차이
Docker와 Kubernetes는 서로 대체하는 기술이 아니라 역할이 다름

- **Docker는 애플리케이션을 Container로 만들고 실행하는데 초점**
```
Spring Boot
    ↓
Docker Image
    ↓
Container
```

- **Kubernetes는 이렇게 만들어진 Container들을 여러 서버에서 운영**
```
Docker
→ Container 생성 및 실행

Kubernetes
→ Container 배포
→ 여러 인스턴스 관리
→ 장애 복구
→ 확장
→ 네트워크 연결
→ 배포 관리
```

실제 서비스 흐름
```
Spring Boot 개발
    ↓
Docker Image 생성
    ↓
Container Registry에 저장
    ↓
Kubernetes 배포
    ↓
Container 실행
```

AWS 환경
```
Spring Boot
    ↓
Docker Image
    ↓
Amazon ECR
    ↓
Amazon EKS
```

## Kubernetes 전체 구조
```
Kubernetes Cluster
│
├── Node 1
│   ├── Pod
│   │   └── Container
│   └── Pod
│       └── Container
│
└── Node 2
    └── Pod
        └── Container
```

| 개념        | 역할                             |
| --------- | ------------------------------ |
| **Cluster**   | Kubernetes 전체 실행 환경            |
| **Node**      | 실제 애플리케이션이 실행되는 서버             |
| **Pod**       | Kubernetes가 애플리케이션을 실행하는 최소 단위 |
| **Container** | 실제 Spring Boot 등의 애플리케이션 프로세스  |

ex) AWS EC2 세 대를 Kubnernetes Worker Node로 사용
```
Kubernetes Cluster

EC2 A → Node
EC2 B → Node
EC2 C → Node
```
각 Node 위에서 여러 Pod가 실행

## Control Plane과 Worker Node
Kubernets Cluster는 크게 **Control Plane과 Worker Node**로 구성
```
Kubernetes Cluster

Control Plane
    │
    ├── Worker Node 1
    ├── Worker Node 2
    └── Worker Node 3
```

### Control Plane
- Cluster 전체를 관리
- 주요 역할
  - API 요청 처리
  - Pod를 실행할 Node 결정
  - 원하는 상태 유지
  - Cluster 상태 저장
- 내부 구성요소
  - API Server
  - Scheduler
  - Controller Manager
  - etcd
```
API Server
→ Kubernetes 요청을 받는 진입점

Scheduler
→ Pod를 어느 Node에서 실행할지 결정

Controller Manager
→ 원하는 상태와 실제 상태를 맞춤

etcd
→ Kubernetes Cluster 상태 저장
```

### Worker Node
- 실제 애플리케이션이 실행되는 서버
- Controle Plane이 전체 Cluster를 관리한다면 Worker Node는 실제 작업을 수행하는 영역
```
Worker Node

Pod
 └── Spring Boot Container
Pod
 └── Spring Boot Container
```

## Pod
Kubernetes에서는 Container를 직접 배포 단위로 사용하지 않고 **Pod라는 단위 안에서 Container를 실행**

```
Pod
 └── Spring Boot Container
```

가장 기본 YAML
```YAML
apiVersion: v1
kind: Pod

metadata:
  name: my-api

spec:
  containers:
    - name: my-api
      image: my-api:1.0
      ports:
        - containerPort: 8080
```
기본적으로 다음 구조가 반복
```
apiVersion:
kind:
metadata:
spec:

apiVersion
→ 사용할 Kubernetes API 버전
kind
→ 생성할 Resource 종류
metadata
→ Resource 이름과 식별 정보
spec
→ Resource가 어떤 상태로 동작해야 하는지 정의
```
YAML 파일을 Kubernetes에 적용할 때 명령
```
kubectl apply -f pod.yaml
```

## 실무에서는 Pod보다 Deployment를 사용
실제 애플리케이션 운영에서는 일반적으로 **Deployment를 통해 Pod를 관리**

Pod 하나만 직접 실행하면 해당 Pod가 사라졌을 때 애플리케이션도 함께 사라질 수 있음

Deployment를 사용하면 Kubernetes에게 다음과 같이 선언 가능
```
Spring Boot Pod를 항상 3개 유지한다.
```
```
Deployment
    ↓
ReplicaSet
    ↓
   Pod
```
```
spec:
  replicas: 3
```
Pod 3개 유지
```
Pod A
Pod B
Pod C

Pod B 장애
    ↓
새로운 Pod 생성
    ↓

Pod A
Pod C
Pod D
```
> 실무에서 "Kubernetes에 애플리케이션을 배포한다"고 할 때는 보통 **Deployment를 생성하고 Deployment가 Pod를 관리하는 구조를 의미**

## kubectl
개발자나 운영자가 Kubernetes Cluster를 조작하기 위해 사용하는 CLI 도구
```
Developer
    ↓
kubectl
    ↓
Kubernetes API Server
    ↓
Cluster
```

### 기본 명령어

Node 확인
- `kubectl get nodes`

Pod 확인
- `kubectl get pods`

Pod 상세 정보 확인
- `kubectl describe pod <pod-name>`

로그 확인
- `kubectl logs <pod-name>`

실시간 로그 확인
- `kubectl logs -f <pod-name>`

Pod 내부 명령 실행
- `kubectl exec -it <pod-name> -- /bin/sh`

YAML 적용
- `kubectl apply -f deployment.yaml`

실무 장애 발생 예시
```
Pod 상태 확인
kubectl get pods
        ↓
상세 상태 확인
kubectl describe pod
        ↓
Application 로그 확인
kubectl logs
```
- 명령어 자체를 모두 외우는 것보다 어떤 문제가 발생했을 때 어떤 순서로 확인하는지를 이해하는 것이 더 중요

## Kubernetes에서 Spring Boot가 실행되는 전체 흐름
```
Spring Boot 개발
        ↓
Docker Image Build
        ↓
Container Registry Push
        ↓
Kubernetes Deployment
        ↓
Pod 생성
        ↓
Container 실행
        ↓
Spring Boot Application 실행
```

Jenkins, GitHub Actions, GitLab CI 같은 CI/CD 도구와 연결되어 자동화되는 경우가 많음
```
Git Push
    ↓
Build / Test
    ↓
Docker Image Build
    ↓
Registry Push
    ↓
Kubernetes Deploy
```
> **내가 개발한 애플리케이션이 Kubernetes에서 어떻게 배포되고, 실행되고, 확장되고, 장애가 났을 때 어떻게 확인하는지를 이해하는 것**

## 핵심 정리
Kubernetes는 Container를 여러 서버에서 안정적으로 운영하기 위한 플랫폼이며, 개발자가 선언한 **Desired State를 실제 상태와 일치시키는 방식**으로 동작
```
Cluster
    ↓
Node
    ↓
Pod
    ↓
Container
```
```
Deployment
    ↓
ReplicaSet
    ↓
Pod
    ↓
Spring Boot Container
```
