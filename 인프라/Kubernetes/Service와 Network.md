---
title: "Service와 Network"
tags:
  - "Service"
  - "Network"
  - "Deployment"
  - "Kubernetes"
---
# Service와 Network

## Service 필요 이유
앞 문서에서 Deployment를 통해 Spring Boot Pod를 여러 개 실행했음
```
Deployment
    ↓
Pod A → 10.244.1.10
Pod B → 10.244.1.11
Pod C → 10.244.2.15
```
- 각 Pod는 Kubernetes 내부에서 고유한 IP를 가짐
- 문제는 **Pod가 영구적인 존재가 아니라는 점**
- Pod 하나가 장애로 삭제되고 새로운 Pod가 생성되면 IP도 바뀔 수 있음

## Service
> 여러 Pod 앞에서 **고정된 접근 지점을 제공하는 Resource**

```
Client
   ↓
Service
   ↓
Pod A
Pod B
Pod C
```
Pod이 바뀌어도 Client 입장에서는 호출 대상이 바뀌지 않음

```
Deployment
→ Pod를 생성하고 유지

Service
→ Pod에 안정적으로 접근할 수 있는 주소 제공
```

## Service에서 Pod 찾기
Service는 Pod 이름을 직접 기억하지 않고 **Label과 Selector를 이용**

ex) Deployment 예시
```yaml
template:
  metadata:
    labels:
      app: my-api
```

ex) Service 예시
```yaml
selector:
  app: my-api
```

그러면 Service는 해당 Label을 가진 Pod를 대상으로 연결
```
Service

selector
app=my-api
    ↓

Pod A
app=my-api

Pod B
app=my-api

Pod C
app=my-api
```

## 기본 Service YAML
Spring Boot Pod가 `8080` Port에서 실행된다고 가정
```yaml
apiVersion: v1
kind: Service

metadata:
  name: my-api-service

spec:
  selector:
    app: my-api

  ports:
    - port: 80
      targetPort: 8080

  type: ClusterIP
```
```
selector
→ 어떤 Pod로 요청을 전달할지 결정

port
→ Service가 요청을 받을 Port

targetPort
→ 실제 Pod Container가 사용하는 Port
```

## Service 생성과 확인

적용
- `kubectl apply -f service.yaml`

확인
- `kubectl get services`
- `kubectl get svc`
```
NAME             TYPE        CLUSTER-IP      PORT(S)
my-api-service   ClusterIP   10.96.120.30    80/TCP
```

상세 정보
- `kubectl describe service my-api-service`

Pod 연결 확인
- `kubectl get endpoints`

## Service를 통한 요청 분산
Service를 통해 요청이 들어오면 Kubernetes 네트워크가 Service 뒤의 Pod들로 트래픽을 전달

따라서 Client가 각 Pod 주소를 알고 있을 필요가 없음

```
Client
   ↓
my-api-service
   ↓
Pod 여러 개
```

중요한 점은 Service가 **고정된 진입점 역할을 함**

## ClusterIP
Service의 기본 Type은 `ClusterIP`

```yaml
type: ClusterIP
```

ClusterIP는 **Kubernetes Cluster 내부에서만 접근 가능한 Service**

```
Frontend Pod
      ↓
Backend Service
      ↓
Backend Pod
```
```
Order Service
      ↓
Payment Service
      ↓
Payment Pod
```
외부 사용자가 접근하는 것이 아니라 Kubernetes **내부 Application끼리 통신**할 때 주로 사용

## Service 이름으로 호출하기
Kubernetes 내부에서는 Service IP를 직접 사용하지 않아도 됨

Kubernetes DNS를 통해 Service 이름으로 접근 가능

```yaml
metadata:
  name: my-api-service
```
```
http://my-api-service:8080 호출
```
```
Order Pod
   ↓
http://user-service
   ↓
User Service
   ↓
User Pod
```

Spring Boot 설정
```yaml
user:
  service:
    url: http://user-service
```

즉 Kubernetes 환경에서는 Pod IP를 코드나 설정에 박아두는 것이 아니라 **Service 이름을 이용해서 다른 Application을 호출하는 구조**가 일반적

## Service Type
| Type         | 용도                          |
| ------------ | --------------------------- |
| **ClusterIP**    | Cluster 내부 통신               |
| **NodePort**  | Node의 특정 Port를 통해 외부 접근     |
| **LoadBalancer** | 외부 Load Balancer를 통해 서비스 공개 |

## NodePort
Worker Node의 특정 Port를 외부에 열어 Service로 연결하는 방식

```
외부 사용자
    ↓
Node IP : NodePort
    ↓
Service
    ↓
Pod
```

```yaml
apiVersion: v1
kind: Service

metadata:
  name: my-api-service

spec:
  type: NodePort

  selector:
    app: my-api

  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080
```
```
http://<Node-IP>:30080
```
```
Client
   ↓
Node:30080
   ↓
Service
   ↓
Pod:8080
```

로컬 학습이나 테스트에서는 유용하지만 실제 운영 서비스에서 사용자에게 직접 노출하는 방식으로는 잘 사용하지 않음

## LoadBalancer
Cloud 환경에서는 `LoadBalancer` Type을 이용해 외부 Load Balancer와 연결할 수 있음

```yaml
spec:
  type: LoadBalancer
```

```
Internet
   ↓
Cloud Load Balancer
   ↓
Kubernetes Service
   ↓
Pod
```

AWS 환경이라면 Kubernetes와 AWS Load Balancer가 연결되는 구조를 만들 수 있음
```
User
 ↓
AWS Load Balancer
 ↓
Kubernetes Service
 ↓
Spring Boot Pod
```
- 따라서 외부 사용자가 Application에 접근해야 할 때 사용할 수 있음
- 다만 여러 서비스가 있으면 각 Service마다 LoadBalancer를 하나씩 만드는 것은 비용이나 관리 측면에서 비효율적일 수 있음
- 실제 서비스에서는 이후 배우게 될 Ingress를 활용해 하나의 외부 진입점에서 여러 Service 요청을 분기하는 구조를 많이 사용

## Service와 Ingress의 차이

#### Service
- Pod 집합에 대한 안정적인 접근점

#### Ingress
- 외부 HTTP 요청을 어떤 Service로 전달할지 결정

```
api.example.com/users
            ↓
       user-service

api.example.com/orders
            ↓
       order-service
```
```
Internet
   ↓
Ingress
   ↓
Service
   ↓
Pod
```

## Namespace가 다른 경우 Service 호출
같은 Namespace라면 보통 Service 이름만 사용하면 됨

하지만 다른 Namespace의 Service를 호출할 수도 있음
```
Service = user-service
Namespace = backend
```
```
user-service.backend.svc.cluster.local
```

전체 Kubernetes Service DNS 형태
```
<Service>.<Namespace>.svc.cluster.local
```

DNS 구조 자체를 외울 필요는 없지만 **Service 이름이 Kubernetes 내부 DNS 주소 역할도 한다**는 점은 알아두는 것이 좋음

## Service가 있는데 Pod 연결이 안 될 때
가장 먼저 확인해야 하는 것이 `selector`와 Pod의 `label`

Deployment의 label과 Service의 selector가 일치하지 않으면 연결할 Pod를 찾지 못함

```
kubectl get pods --show-labels

kubectl describe service my-api-service

kubectl get endpoints
```

## Port를 잘못 설정한 경우
Service의 `targetPort`와 Application Port가 다른 경우
```
Service
   ↓
8081 전달
   ↓
Spring Boot는 8080에서 실행
   ↓
연결 실패
```

## 실무 요청 흐름
Deployment에서 Pod 세 개 실행
```
my-api Pod A
my-api Pod B
my-api Pod C
```

Service
```
my-api-service
```

전체 구조
```
Client
   ↓
Service
   ↓
Pod A
Pod B
Pod C
```

Pod 하나가 죽으면 Deployment가 새로운 Pod를 만듬

Service는 새로운 Pod까지 대상으로 사용

따라서 외부 Client나 다른 Backend Service 입장에서는 Pod가 교체되었는지 알 필요가 없음

## 백엔드 서비스 간 통신 예시
```
Order Service
User Service
Payment Service
```

```
Order Deployment
   ↓
Order Pods
   ↑
Order Service


User Deployment
   ↓
User Pods
   ↑
User Service


Payment Deployment
   ↓
Payment Pods
   ↑
Payment Service
```

```
http://user-service
```

## 실무 명령어

Service 확인
- `kubectl get services`
- `kubectl get svc`

Service 상세 확인
- `kubectl describe service my-api-service`

Pod Label 확인
- `kubectl get pods --show-labels`

특정 Label을 가진 Pod 확인
- `kubectl get pods -l app=my-api`

Endpoint 확인
- `kubectl get endpoints`

EndpointSlice 확인
- `kubectl get endpointslices`

Service 설정 YAML 확인
- `kubectl get service my-api-service -o yaml`

네트워크 문제 발생
```
Service 존재 여부
    ↓
kubectl get svc

Selector 확인
    ↓
kubectl describe service

Pod Label 확인
    ↓
kubectl get pods --show-labels

Endpoint 연결 확인
    ↓
kubectl get endpoints

Application Port 확인
```

## 실무 상황 예시

### 1. Pod는 정상인데 API 호출이 안 됨

Pod 상태 확인
- `kubectl get pods`

Service 확인
- `kubectl get svc`
- `kubectl describe service my-api-service`

Service가 Pod를 제대로 찾는지 확인
- `kubectl get endpoints`

### 2. Spring는 8080에서 실행되는데 Service 연결이 안 됨

Service 설정 확인
```yaml
ports:
  - port: 80
    targetPort: 8080
```
- targetPort가 Spring의 실제 Port와 일치해야 함

### 3. 다른 Backend Service에서 호출해야 함

Pod IP가 아니라 Service 이름 사용
```
http://user-service
```

### 4. 외부에서 Application에 접근해야 함

ClusterIP는 Cluster 내부에서만 사용할 수 있음
```
NodePort
LoadBalancer
Ingress
```
- Ingress와 LoadBalancer 구조가 더 중요

## 핵심 정리

### Service는 Pod 앞에서 고정된 접근 지점을 제공
```
Client
   ↓
Service
   ↓
Pod
Pod
Pod
```

### Service는 `selector`를 이용해 특정 Label을 가진 Pod를 찾음
```
Service Selector
app=my-api
   ↓
Pod Label
app=my-api
```

### Service Type은 우선 다음 정도만 구분
```
ClusterIP
→ Cluster 내부 접근

NodePort
→ Node Port를 통한 외부 접근

LoadBalancer
→ 외부 Load Balancer 연결
```

### 흐름
```
Deployment
→ Pod 생성 및 유지

Service
→ Pod에 안정적인 접근 주소 제공

Service 이름
→ Kubernetes 내부 서비스 간 통신에 사용
```

### 네트워크 문제 순서 확인
```
Pod 정상 여부
    ↓
Service 존재 여부
    ↓
Selector / Label 확인
    ↓
Endpoint 확인
    ↓
targetPort 확인
```
