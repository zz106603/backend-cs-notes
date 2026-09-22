---
title: "Kubernetes Ingress, HPA와 실제 서비스 구조"
tags:
  - "Ingress"
  - "HPA"
---
# Kubernetes Ingress, HPA와 실제 서비스 구조

## Kubernetes에서 외부 요청 전달

일반적인 ClusterIP Service는 Kubernetes Cluster 내부에서만 접근 가능

외부 사용자가 실제 서비스에 접근하려면 외부 요청을 Cluster  내부의 Service까지 전달하는 구조가 필요

```
외부 사용자
    ↓
Load Balancer
    ↓
Ingress Controller
    ↓
Service
    ↓
Spring Boot Pod
```

| 구성요소 | 역할 |
| --- | --- |
| Load Balancer | 외부 요청을 Kubernetes의 진입접으로 전달 |
| Ingress | 도메인과 URL 경로에 따른 HTTP/HTTPS 라우팅 규칙 정의 |
| Ingress Controller | Ingress 규칙을 실제 트래픽 처리에 반영 |
| Service | 요청을 전달할 Pod 집합에 안정적인 접근점 제공 |
| Pod  | Spring Boot 애플리케이션 실행 |

- 여기서 중요한 점은 **Ingress가 Pod에 직접 연결되는 것이 아니라 Service를 통해 연결**
- Deployment가 Pod를 새로 생성하거나 Scale Out하더라도 Ingress 설정에서 개별 Pod 주소를 수정할 필요가 없음

## Ingress
> 외부에서 들어오는 HTTP/HTTPS 요청을 어떤 Service로 전달할지 정의하는 Kubernetes Resource

ex) 하나의 Kubernetes Cluster에서 두 개의 백엔드 서비스를 운영
```
api.example.com/users
    ↓
user-service
    ↓
User Pod

api.example.com/orders
    ↓
order-service
    ↓
Order Pod
```
- Ingress에서는 URL 경로에 따라 요청을 서로 다른 Service로 전달하도록 설정할 수 있음
- 도메인에 따라서도 구분할 수 있음

```
user.example.com
    ↓
user-service

order.example.com
    ↓
order-service
```
- 각 애플리케이션마다 별도의 외부 Load Balancer를 생성하지 않고, 하나의 외부 진입점에서 여러 Service로 요청을 분기하는 구조 가능
- 실무에서는 사용자 요청을 전달할 때뿐 아니라 HTTPS 인증서 연결, 도메인 기반 라우팅 등을 구성할때도 사용

## Ingress Controller와 Ingress YAML
Ingress를 생성했다고 해서 외부 요청이 자동으로 전달되는 것은 아님

- Ingress는 요청을 어디로 전달할지 정의한 설정
- 실제로 해당 규칙을 처리하는 구성요소가 Ingress Controller
- 따라서 Ingress를 사용하려면 Cluster에 적절한 Controller가 설치되어 있어야 함
- 대표적으로 NginX 기반 Controller나 클라우드 환경에 맞는 Controller 사용

```
kubectl get ingressclass
```
IngressClass는 어떤 Controller가 Ingress를 처리할지 지정하는 데 사용

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress

metadata:
  name: my-api-ingress

spec:
  ingressClassName: nginx

  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: my-api-service
                port:
                  number: 80
```

| 설정 | 의미 |
| --- | --- |
| ingressClassName | Ingress를 처리할 Controller 지정 |
| host | 요청을 받을 도메인 |
| path | 요청 URL 경로 |
| pathType | 경로를 비교하는 방식 |
| backend.service.name | 요청을 전달할 Service |
| backend.service.port | 요청을 전달할 Service Port |

최종 요청 흐름
```
http://api.example.com/users
          ↓
Ingress Controller
          ↓
my-api-service:80
          ↓
Spring Boot Pod:8080
          ↓
GET /users
```
- Ingress는 기본적으로 URL 경로를 자동으로 제거하거나 변경하지 않음
- 따라서 `/users` 요청은 별도의 경로 재작성 설정이 없다면 Spring에도 그대로 전달
- Ingress를 실제로 사용하려면 Controller 설치와 외부 접근 경로 구성 외에도 도메인이 해당 진입점을 가리키도록 DNS 설정이 필요

## HTTPS와 TLS
실제 운영 서비스에서는 일반적으로 HTTP가 아니라 HTTPS를 사용

Ingress는 **TLS 인증서를 연결하여 HTTPS 요청을 처리**하도록 구성할 수 있음

ex) TLS 인증서와 개인키가 Kubernetes Secret에 저장
```yaml
spec:
  tls:
    - hosts:
        - api.example.com
      secretName: my-api-tls
```
일반적인 TLS 종료 구조
```
Client
    ↓
HTTPS
    ↓
Ingress Controller
    ↓
HTTP
    ↓
Service
    ↓
Spring Boot Pod
```
- TLS 종료 위치와 내부 통신 방식은 사용하는 Controller 및 인프라 설정에 따라 달라질 수 있음
- 내부 구간까지 암호화해야 한다면 별도의 HTTPS 구성이 필요

## HPA
지금까지 Deployment에서 Pod 개수를 변경할 때 `replicas`값을 직접 수정함
```
replicas: 3
```

하지만 실제 서비스에서는 시간대나 사용자 수에 따라 트래픽이 달라질 수 있음
```
평상시

Spring Boot Pod × 3
        ↓
트래픽 증가
        ↓
Spring Boot Pod × 5
        ↓
트래픽 감소
        ↓
Spring Boot Pod × 3
```

> **HPA(Horizontal Pod Autoscaler)는 CPU, Memory 등의 지표를 기준으로 Pod 개수를 자동으로 조절하는 Kubernetes 기능**

- HPA는 Deployment와 같은 확장 가능한 Resource의 Replica 수를 변경하는 방식으로 동작

```
HPA
 ↓
Deployment의 replicas 조정
 ↓
ReplicaSet
 ↓
Pod 개수 증가 또는 감소
```
- **Deployment는 설정된 개수의 Pod를 유지**
- **HPA는 현재 부하에 따라 Deployment가 유지할 Pod 개수를 조정**

## HPA 기준
> 가장 기본적인 방식은 CPU 사용률을 기준으로 판단하는 것
```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"

  limits:
    cpu: "500m"
    memory: "512Mi"
```
HPA의 CPU 목표 사용률을 60%로 설정했다면, 이 60%는 CPU Limit이 아니라 **CPU Request를 기준으로 계산**

```
CPU Request = 250m

목표 사용률 = 60%

목표 CPU 사용량 = 150m
```
- 평균 CPU 사용률이 목표보다 높아지면 HPA는 Pod수를 늘려 부하를 분산하려 함
- 반대로 평균 사용률이 낮아지면 필요하지 않은 Pod를 줄일 수 있음
- 다만 CPU 사용률이 목표치를 초과했다고 해서 즉시 Pod가 하나씩 증가하는 것은 아님
- Kubernetes는 주기적으로 지표를 확인하고 현재 사용률과 목표 사용률의 차이를 계산해 필요한 Replica 수를 결정
- 또한 불필요한 증감이 반복되지 않도록 허용 오차와 안정화 동작을 적용

## HPA YAML 작성
ex) Pod 개수를 최소 2개에서 최대 6개까지 조절하고, 평균 CPU 사용률을 60% 수준으로 유지

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler

metadata:
  name: my-api-hpa

spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-api

  minReplicas: 2
  maxReplicas: 6

  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```
| 설정 | 의미 |
| --- | --- |
| scaleTargetRef | 자동 확장할 Deployment 지정 |
| minReplicas | 최소 Pod 개수 |
| maxReplicas | 최대 Pod 개수 |
| metrics | 자동 확장의 기준이 되는 지표 |
| averageUtilization | 목표 평균 사용률 |

## HPA 적용 및 상태 확인

작성한 YAML을 `hpa.yaml`로 저장하고 적용
- `kubectl apply -f hpa.yaml`

HPA 상태 확인
- `kubectl get hpa`

```
NAME         REFERENCE          TARGETS   MINPODS   MAXPODS   REPLICAS
my-api-hpa   Deployment/my-api  45%/60%   2         6         3
```
- 현재 평균 CPU 사용률을 45%, 목표 사용률을 60%
- REPLICAS는 현재 Pod 개수

부하가 증가하면 Pod 개수가 변경되는지 확인
- `kubectl get pods -w`

실제 CPU 및 Memory 사용량 확인
- `kubectl top pods`

HPA의 상세 정보와 이벤트 확인
- `kubectl describe hpa my-api-hpa`

## HPA를 사용하기 위한 조건과 주의사항
HPA를 생성했다고 해서 모든 환경에서 자동 확장이 바로 동작하는 것은 아님

- CPU 기반 HPA가 정상적으로 동작하려면 Kubernetes가 Pod의 CPU 사용량을 수집해야 함
- 일반적으로 Metrics Server를 사용
- 또한 CPU 사용률을 Request 기준으로 계산하므로 대상 Pod의 Container에 적절한 CPU Request가 설정되어 있어야 함

```
Metrics Server
    ↓
Pod CPU 사용량 수집
    ↓
HPA
    ↓
Deployment replicas 조정
    ↓
Pod 개수 변경
```
```
kubectl top pods
kubectl get hpa
kubectl describe hpa my-api-hpa
```
- CPU 지표가 조회되지 않는다면 Metric Server 및 Resource Request 설정 확인
- HPA를 적용한 Deplyoment의 Replica 수는 HPA가 관리하도록 두는 것이 좋음

## HPA가 Pod를 늘려도 Node가 부족할 때

HPA는 Pod 개수를 조절하는 기능

Worker Node를 자동으로 추가하는 기능은 아님

ex) Worker Node에 Pod를 4개까지 실행할 수 있는 여유 자원
```
Worker Node

Pod A
Pod B
Pod C
Pod D
```
- HPA가 Pod를 6개로 늘리도록 결정하더라도 Node에 충분한 자원이 없다면 새로운 Pod가 정상적으로 배치되지 못하고 Pending
- **Cluster Autoscaler 같은 별도의 Node 자동 확장 기능이나 인프라 증설이 필요**

```
트래픽 증가
    ↓
HPA가 Pod 증가 결정
    ↓
Node 자원 부족
    ↓
새로운 Pod Pending
    ↓
Node 추가 필요
```
- HPA를 사용한다고 해서 서버 자원이 무제한으로 확장되는 것은 아님

## Spring Boot 서비스 운영 구조

```
                 Internet
                    ↓
               Load Balancer
                    ↓
             Ingress Controller
                    ↓
             my-api-service
                    ↓
         ┌──────┼──────┐
         ↓          ↓          ↓
       Pod A      Pod B      Pod C
         │          │          │
         └──────┼──────┘
                    ↓
              외부 DB / Redis
```
- **Deployment**는 Spring Boot Pod를 생성하고 유지
- **Service**는 Pod들이 교체되더라도 안정적인 접근점 제공
- **Ingress**는 외부 HTTP/HTTPS 요청을 Service로 전달
- **HPA**는 이 서비스의 CPU 사용률 등을 확인하면서 Deployment의 Replica 수를 조절

```
트래픽 증가
    ↓
CPU 사용률 증가
    ↓
HPA가 Replica 수 증가
    ↓
Deployment가 새로운 Pod 생성
    ↓
Readiness Probe 성공
    ↓
새로운 Pod가 Service의 요청 처리 대상에 포함
```

## 운영 발생 문제

### 1. 외부에서 접속했는데 404 또는 502, 503 오류 발생
- Ingress에서 요청이 올바른 Service로 전달되고 있느지 호가인

```
kubectl get ingress
kubectl describe ingress my-api-ingress
kubectl get svc
kubectl get pods
```
- Ingress Controller 정상 설치 확인
- Ingress의 도메인과 경로가 요청에 일치하는지 확인
- Service의 Endpoint와 Pod의 Readiness 상태 확인

### 2. 트래픽이 증가했는데 Pod 개수가 늘어나지 않음
```
kubectl get hpa
kubectl describe hpa my-api-hpa
kubectl top pods
```
- HPA의 목표 사용률과 실제 사용률 비교, CPU 지표가 정상 수집되는지 확인
- HPA의 maxReplicas에 도달한 상태라면 CPU 사용률이 증가해도 최대 개수를 초과하지 않음

### 3. HPA가 Pod를 늘렸는데 새로운 Pod가 실행되지 않음
```
kubectl get pods
kubectl describe pod <pod-name>
kubectl get nodes
```
- 새로운 Pod가 Pending 상태라면 Node 자원이 부족하거나 Pod 배치 조건을 충족하지 못하는 상황인지 확인

## 핵심 정리
- Ingress는 외부 HTTP/HTTPS 요청을 Service로 전달하는 라우팅 규칙을 정의
- Ingress Controller는 해당 규칙을 실제로 처리
- HPA는 CPU 등의 지표를 기준으로 Deployment의 Pod 개수를 자동으로 조절

```
Ingress
→ 외부 요청의 전달 경로 관리

Service
→ Pod에 안정적인 접근점 제공

Deployment
→ Pod 생성 및 유지

HPA
→ 부하에 따라 Pod 개수 조절
```

```
외부 사용자
    ↓
Ingress Controller
    ↓
Service
    ↓
Spring Boot Pod

트래픽 증가
    ↓
HPA가 Pod 개수 조절
    ↓
Service가 새로운 Pod를 요청 처리 대상으로 사용
```
