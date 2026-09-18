---
title: "Deployment와 Pod 운영"
tags:
  - "Deployment"
  - "Pod"
  - "Kubernetes"
---
# Deployment와 Pod 운영

## Deployment

Kubernetes에서 실제 애플리케이션은 Pod 안에서 실행됨

```
Pod
 └── Spring Boot Container
```
Pod을 직접 생성하는 것도 가능
```yaml
apiVersion: v1
kind: Pod

metadata:
  name: my-api

spec:
  containers:
    - name: my-api
      image: my-api:1.0
```
Pod 자체는 일시적인 Resource
- Pod -> 장애 발생 -> Pod 종료
- 단순히 Pod만 생성하면 애플리케이션을 일정 개수로 유지해줄 상위 관리 주체가 없음

요구사항 예시
```
Spring Boot 서버를 항상 3개 유지하고 싶다.
하나가 죽으면 자동으로 다시 생성하고 싶다.
사용자가 많아지면 5개로 늘리고 싶다.
새 버전으로 서비스 중단 없이 교체하고 싶다.
```

## Deployment / ReplicaSet / Pod 관계
Deployment를 생성하면 내부적으로 Pod를 직접 하나씩 관리하는 것이 아니라 ReplicaSet을 통해 Pod를 관리

```
Deployment
    ↓
ReplicaSet
    ↓
Pod
```
| Resource   | 역할                |
| ---------- | ----------------- |
| Deployment | 배포 방식과 버전 관리      |
| ReplicaSet | 지정된 개수의 Pod 유지    |
| Pod        | 실제 Application 실행 |

## Deployment YAML
```yaml
apiVersion: apps/v1
kind: Deployment

metadata:
  name: my-api

spec:
  replicas: 3

  selector:
    matchLabels:
      app: my-api

  template:
    metadata:
      labels:
        app: my-api

    spec:
      containers:
        - name: my-api
          image: my-api:1.0
          ports:
            - containerPort: 8080
```
```
replicas
→ Pod 몇 개를 유지할 것인가

selector
→ 어떤 Pod를 이 Deployment가 관리할 것인가

template
→ 생성할 Pod의 구조는 무엇인가
```

### replicas
- `replicas: 3`
- Spring BOot Pod를 세 개 유지하겠다는 의미
- 하나가 죽더라도 kubernetes가 다시 생성해서 세 개를 맞춤

### selector와 labels
```yaml
selector:
  matchLabels:
    app: my-api
```
```yaml
template:
  metadata:
    labels:
      app: my-api
```
Deployment는 `label`을 기준으로 자신이 관리할 Pod를 찾음
```
Deployment

selector
app=my-api
    ↓
app=my-api 라벨을 가진 Pod 관리
```

### template
Deployment가 새 Pod를 만들 때 사용할 설계도
```yaml
template:
  metadata:
    labels:
      app: my-api

  spec:
    containers:
      - name: my-api
        image: my-api:1.0
```
Pod가 하나 사라져 새로 생성해야 할 때도 이 template를 기준으로 새로운 Pod를 만듬


## Deployment 배포

kubernetes 적용
```
kubectl apply -f deployment.yaml
```

Deployment 확인
```
kubectl get deployments
```

Pod 확인
```
kubectl get pods
```

정상 생성
```
NAME                      READY   STATUS    RESTARTS
my-api-7d9f8c7c95-a1b2c   1/1     Running   0
my-api-7d9f8c7c95-d3e4f   1/1     Running   0
my-api-7d9f8c7c95-g5h6i   1/1     Running   0
```

Deployment 상태 확인
```
kubectl get deployment my-api

NAME     READY   UP-TO-DATE   AVAILABLE
my-api   3/3     3            3
```

## Pod 죽음

Pod 확인
```
kubectl get pods
```

Pod 하나를 선택해서 삭제
```
kubectl delete pod <pod-name>
```

확인
```
kubectl get pods
```

- 새로운 Pod이 새로 생성
- 특정 Pod를 살려내는 것이 아니라 필요한 경우 새로운 Pod를 생성

## Scale Out
트래픽이 증가하면 애플리케이션 인스턴스 수를 늘려야할 수 있음

기존 `replicas: 3`을 5개로 늘리는 방법

### YAML 수정
```
replicas: 5

kubectl apply -f deployment.yaml
```

### kubectl 명령어 사용
```
kubectl scale deployment my-api --replicas=5

kubectl get pods
```
실무에서는 Git으로 Kubernetes YAML을 관리한다면 **최종 설정값은 YAML에 반영해두는 것이 중요**

CLI로만 변경하면 Git에 저장된 설정과 실제 Cluster 상태가 달라질 수 있음

## 새 버전 배포
ex) 애플리케이션 수정
- 기존 Image = `my-api:1.0`
- 새 버전 Image = `my-api:2.0`

Deployment의 Image를 변경
```yaml
containers:
  - name: my-api
    image: my-api:2.0
```
```
kubectl apply -f deployment.yaml
```
Kubernetes는 기존 Pod를 모두 한 번에 제거하고 새로운 Pod를 만드는 방식이 아니라 기본적으로 **Rolling Update 방식**으로 교체

## Rolling Update
Kubernetes는 단계적으로 교체
- 새로운 v2 Pod를 생성
- 새 Pod가 정상적으로 올라오면 기존 Pod를 하나 제거
- 반복

서비스 전체를 중단하고 새 버전을 올리는 것이 아니라 **기존 버전을 유지하면서 점진적으로 새 버전으로 교체하는 방식**
- Kubernetes를 이용하면 무중단 배포 구조를 구성하기 쉬워짐
- 다만 실제로 완전한 무중단 배포를 하려면 이후에 배우는 **Readiness Probe**도 중요
- 새 Pod의 애플리케이션이 완전히 준비되기 전에 트래픽이 전달되면 문제가 발생할 수 있기 때문임

## 배포 상태 확인
```
kubectl rollout status deployment/my-api

-> deployment "my-api" successfully rolled out
```
현재 Deployment가 사용하는 Image 확인
```
kubectl describe deployment my-api

kubectl get deployment my-api -o wide
```
```
Deployment 적용
    ↓
Rollout 상태 확인
    ↓
Pod 상태 확인
    ↓
Application 로그 확인
```

## Rollback
- v1 정상 운영
- v2 배포
- Application Error 발생

배포 기록 확인
```
kubectl rollout history deployment/my-api
```

Deployment를 이전 버전으로 되돌림
```
kubectl rollout undo deployment/my-api
```

상태 확인
```
kubectl rollout status deployment/my-api

kubectl get pods
```

```
v1
 ↓
v2 배포
 ↓
장애 발견
 ↓
kubectl rollout undo
 ↓
v1으로 복구
```

## ReplicaSet을 직접 조작하지 않는 이유
Deployment를 사용하면 내부적으로 ReplicaSet이 생성

```
kubectl get replicasets
```

```
Deployment
    ↓
ReplicaSet
    ↓
Pod × 3
```

새 버전 배포
```
Deployment
│
├── ReplicaSet v1
│      └── 기존 Pod
│
└── ReplicaSet v2
       └── 신규 Pod
```
Rolling Update 과정에서 새로운 ReplicaSet의 Pod는 증가하고 기존 ReplicaSet의 Pod는 감소
```
v1 ReplicaSet
3 → 2 → 1 → 0

v2 ReplicaSet
0 → 1 → 2 → 3
```

- 이 구조 덕분에 Deployment가 버전 교체와 Rollback을 관리할 수 있음
- 다만 백엔드 개발자가 ReplicaSet을 직접 생성하거나 수정할 일은 많지 않음

```
Deployment를 관리한다.
ReplicaSet은 Deployment가 관리한다.
Pod는 ReplicaSet이 유지한다.
```

## Image 변경
YAML 파일을 직접 수정하지 않고 명령어로 Image를 변경할 수도 있음

```bash
kubectl set image deployment/my-api \
my-api=my-api:2.0

kubectl rollout status deployment/my-api
```

CI/CD 환경 구조 예시
```
Git Push
    ↓
Spring Boot Build
    ↓
Docker Image 생성
my-api:20260916-01
    ↓
Registry Push
    ↓
Deployment Image 변경
    ↓
Rolling Update
```

## 실무에서 Image Tag는 latest만 사용하지 않는 것이 좋음
```yaml
image: my-api:latest
```
운영에서는 버전을 명확하게 구분할 수 있는 Image Tag를 사용하는 것이 좋음

```
my-api:1.0.0
my-api:1.0.1
my-api:20260916-01
my-api:<git-commit-sha>
```
- 어떤 버전이 실제로 배포되었는지 추적하기 쉬워지기 때문임

CI/CD 환경에서는 Git Commit SHA를 Image Tag로 사용하는 방식도 자주 볼 수 있음
```
Git Commit
abc123
↓
Docker Image
my-api:abc123
```

## Deployment 운영 시 자주 사용하는 명령어

Deployment 확인
```
kubectl get deployments
```

특정 Deployment 상세 확인
```
kubectl describe deployment my-api
```

Pod 확인
```
kubectl get pods
```

ReplicaSet 확인
```
kubectl get replicasets
```

Scale Out
```bash
kubectl scale deployment my-api --replicas=5
```

배포
```
kubectl rollout status deployment/my-api

kubectl rollout history deployment/my-api

kubectl rollout undo deployment/my-api
```

Image 변경
```
kubectl set image deployment/my-api \
my-api=my-api:2.0
```

```
배포

kubectl apply
    ↓

상태 확인

kubectl get deployments
kubectl get pods
    ↓

새 버전 배포

kubectl rollout status
    ↓

문제 발생

kubectl rollout history
kubectl rollout undo
```

## 핵심 정리
실제 Kubernetes 애플리케이션 운영에서는 Pod를 직접 관리하기보다 Deployment를 사용

```
Deployment
    ↓
ReplicaSet
    ↓
Pod
    ↓
Container
    ↓
Spring Boot
```

**Deployment 역할**
- Pod 개수 유지
- Self-Healing
- Scale Out
- Rolling Update
- Rollback

```
Deployment 배포
    ↓
Pod 상태 확인
    ↓
replicas 변경
    ↓
새 Image 배포
    ↓
Rolling Update 확인
    ↓
문제 발생 시 로그 확인
    ↓
Rollback
```
