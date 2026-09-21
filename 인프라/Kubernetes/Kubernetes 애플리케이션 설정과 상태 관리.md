---
title: "Kubernetes 애플리케이션 설정과 상태 관리"
tags:
  - "Kubernetes"
  - "Config"
  - "Probe"
  - "Resource"
---
# Kubernetes 애플리케이션 설정과 상태 관리

## 애플리케이션 설정

Spring Boot 애플리케이션은 보통 환경에 따라 설정이 달라짐

```
개발 환경
DB_HOST=dev-db
DB_PORT=5432

운영 환경
DB_HOST=prod-db
DB_PORT=5432
```
DB 계정이나 외부 API Key도 환경마다 달라질 수 있음

Docker Image 안에 이런 값을 직접 넣어버리면 문제가 생김
```
Docker Image
 ├── Application
 ├── DB 주소
 ├── DB 계정
 └── API Key
```
- 설정값 하나를 바꾸기 위해 Image를 다시 만들어야 하고, 비밀번호 같은 민감정보까지 Image에 포함될 수 있음
- 그래서 Kubernetes에서는 보통 **애플리케이션과 설정을 분리**

```
Application Image + ConfigMap + Secret

ConfigMap
→ 일반 설정값

Secret
→ 비밀번호, Token, Key 등 민감한 설정
```

## ConfigMap
> 애플리케이션에서 사용하는 **일반적인 설정값을 Kubernetes에서 별도로 관리하는 Resource**

ex) Spring Boot
```yaml
spring:
  profiles:
    active: prod

external:
  api:
    url: https://api.example.com
```
```yaml
apiVersion: v1
kind: ConfigMap

metadata:
  name: my-api-config

data:
  SPRING_PROFILES_ACTIVE: prod
  EXTERNAL_API_URL: https://api.example.com
```
적용
- `kubectl apply -f configmap.yaml`

확인
- `kubectl get configmaps`
- `kubectl get cm`

내용 확인
- `kubectl describe configmap my-api-config`


## ConfigMap을 Container 환경변수로 주입하기

ConfigMap을 만들었다고 해서 자동으로 Spring Boot에 전달되는 것은 아님

Deployment에서 ConfigMap 값을 Container 환경변수로 연결해야 함

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

          env:
            - name: SPRING_PROFILES_ACTIVE
              valueFrom:
                configMapKeyRef:
                  name: my-api-config
                  key: SPRING_PROFILES_ACTIVE

            - name: EXTERNAL_API_URL
              valueFrom:
                configMapKeyRef:
                  name: my-api-config
                  key: EXTERNAL_API_URL
```
```
ConfigMap

SPRING_PROFILES_ACTIVE=prod
EXTERNAL_API_URL=https://api.example.com
        ↓
    Deployment
        ↓
Container Environment Variable
        ↓
    Spring Boot
```

## ConfigMap 전체를 한 번에 주입하기
설정이 많다면 하나씩 작성하지 않고 ConfigMap 전체를 환경변수로 주입할 수도 있음
```yaml
envFrom:
  - configMapRef:
      name: my-api-config
```
```yaml
data:
  SPRING_PROFILES_ACTIVE: prod
  EXTERNAL_API_URL: https://api.example.com
  LOG_LEVEL: INFO
```
- 설정이 많을 때는 `envFrom` 방식이 간단함
- 다만 여러 ConfigMap이나 Secret을 함께 사용하는 환경에서는 어떤 값이 어디에서 들어오는지 명확하게 관리하는 것이 중요

## Secret
DB Password, API Key, Token 같은 값을 ConfigMap에 넣는 것은 적절하지 않음

```yaml
apiVersion: v1
kind: Secret

metadata:
  name: my-api-secret

type: Opaque

stringData:
  DB_USERNAME: app-user
  DB_PASSWORD: password123
```

- `kubectl apply -f secret.yaml`
- `kubectl get secrets`
- `kubectl describe secret my-api-secret`
  - describe에서는 일반적으로 Secret 값 자체는 그대로 출력되지 않음
 
## Secret을 환경변수로 사용하기
Deployment에서 ConfigMap과 비슷한 방식으로 연결

```yaml
env:
  - name: DB_USERNAME
    valueFrom:
      secretKeyRef:
        name: my-api-secret
        key: DB_USERNAME

  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: my-api-secret
        key: DB_PASSWORD
```
Spring Boot
```yaml
spring:
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

전체 구조
```
Secret

DB_USERNAME
DB_PASSWORD
    ↓
Deployment
    ↓
Container Environment Variable
    ↓
Spring Boot Datasource
```

## ConfigMap과 Secret 구분
| 구분    | ConfigMap               | Secret                   |
| ----- | ----------------------- | ------------------------ |
| 용도    | 일반 설정                   | 민감한 설정                   |
| 예시    | URL, Profile, Log Level | Password, Token, API Key |
| 사용 방식 | 환경변수 / 파일               | 환경변수 / 파일                |

```
ConfigMap

SPRING_PROFILES_ACTIVE=prod
REDIS_HOST=redis-service
LOG_LEVEL=INFO


Secret

DB_PASSWORD
JWT_SECRET
API_KEY
```
- 다만 Kubernetes Secret이라고 해서 값 자체가 자동으로 강력하게 암호화되는 것은 아님
- Secret에서 사용하는 Base64 표현은 **암호화가 아니라 Encoding**

실제 운영 환경에서는 아래를 고려
```
Kubernetes Secret
+
접근 권한 제어
+
etcd Encryption
+
외부 Secret Manager
```

## ConfigMap이나 Secret 수정 및 반영
환경변수 형태로 ConfigMap이나 Secret을 주입했다면 값을 변경한다고 기존 Container의 환경변수가 바로 바뀌지않음

이미 실행중인 Container의 환경변수는 그대로일 수 있음

일반적으로 Pod를 새로 생성하도록 Deployment를 재시작
- `kubectl rollout restart deployment/my-api`
- `kubectl rollout status deployment/my-api`
```
ConfigMap 수정
    ↓
Deployment Restart
    ↓
새 Pod 생성
    ↓
새 ConfigMap 값 주입
```

## Kubernetes는 애플리케이션이 정상인지 어떻게 판단하는가
Container가 실행되고 있다고 해서 실제 Application이 정상이라는 뜻은 아님

ex) Process는 살아 있지만 DB 연결 문제로 실제 API를 처리하지 못하는 상황

- Kubernetes에서는 이를 확인하기 위해 **Probe**를 사용
  - Liveness Probe
  - Readiness Probe
  - Startup Probe

## Liveness Probe
> 이 애플리케이션이 살아 있는가?

ex) Spring Boot가 내부 오류 때문에 응답하지 않는 상태
- Liveness Probe가 계속 실패하면 Kubernetes가 Container를 재시작할 수 있음

```
Liveness 실패
    ↓
Container 비정상 판단
    ↓
Container Restart
```
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080

  initialDelaySeconds: 30
  periodSeconds: 10
```
- Container 시작 후 30초 대기
- 10초마다 /actuator/health/liveness 호출
- 계속 실패하면 Container를 다시 시작

## Readiness Probe
> 이 Pod가 지금 요청을 받을 준비가 되었는가?

ex) Spring Boot 시작이 15초, Container 자체는 실행되었지만 Spring Boot가 초기화 중
- 바로 사용자 요청을 전달하면 오류 발생
- Readiness Probe가 성공하기 전까지는 Service의 트래픽 대상에서 제외

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080

  initialDelaySeconds: 10
  periodSeconds: 5
```
Readiness는 **Container를 죽이는 목적보다 트래픽을 받을 수 있는지 판단하는 데 사용**


## Liveness와 Readiness 차이
```
Liveness

"얘가 살아 있나?"

실패 지속
→ Container Restart
```
```
Readiness

"얘한테 지금 요청 보내도 되나?"

실패
→ Service 요청 대상에서 제외
```

ex) DB가 잠시 사용할 수 없음
- Application 자체를 다시 시작할 필요는 없지만 API 요청을 정상적으로 처리할 수 없다면 Readiness를 실패하도록 구성
- Pod는 살아있지만, Service 트래픽에서는 제외

## Startup Probe
Spring Boot 애플리케이션의 시작 시간이 긴 경우가 있음

ex) 시작하는 데 60초가 필요한데 Liveness Probe가 너무 빨리 동작하면 문제가 발생
- Application이 제대로 시작하기도 전에 계속 재시작될 수 있음
- 이를 방지할 때 Startup Probe를 사용할 수 있음

```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080

  periodSeconds: 5
  failureThreshold: 12
```
- 5초 x 12번
- 최대 약 60초 동안 시작 대기

## Spring Boot Actuator와 Probe
Spring Boot에서는 Actuator를 이용하면 Kubernetes Probe와 연결하기 편함

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

```
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

```
Kubernetes

Readiness Probe
    ↓
Spring Boot Actuator
    ↓
Application 상태 확인
```

## Probe 설정 전체 예시
```yaml
containers:
  - name: my-api
    image: my-api:1.0

    ports:
      - containerPort: 8080

    startupProbe:
      httpGet:
        path: /actuator/health
        port: 8080
      periodSeconds: 5
      failureThreshold: 12

    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      periodSeconds: 10

    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      periodSeconds: 5
```
```
Pod 생성
   ↓
Startup Probe
   ↓
Application 시작 완료
   ↓
Liveness Probe
→ 계속 살아 있는지 확인

Readiness Probe
→ 요청 받을 준비가 되었는지 확인
```

## Resource Request와 Limit
Kubernetes Cluster의 Worker Node는 CPU와 Memory가 제한되어 있음

아무런 제한 없이 Application이 Memory를 사용하면 하나의 Pod가 Node의 자원을 과도하게 사용할 수 있음

이를 관리하기 위해 `requests`와 `limits`를 설정할 수 있음
```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"

  limits:
    cpu: "500m"
    memory: "512Mi"
```

## Resource Request
> 이 Pod를 실행하기 위해 최소한 이 정도 Resource는 필요함

```yaml
requests:
  cpu: "250m"
  memory: "256Mi"
```
Scheduler는 이 값을 참고해서 Pod를 어느 Node에 배치할지 결정

```
Pod가 요구하는 Memory
256Mi

Node의 남은 Memory
100Mi
```
- 해당 Node에는 Pod를 배치하기 어려움
- 따라서 Request는 **Scheduling과 Resource 보장에 관련된 값**

## Resource Limit
> 해당 Container가 사용할 수 있는 자원의 상한선

```yaml
limits:
  cpu: "500m"
  memory: "512Mi"
```
- CPU와 Memory는 제한을 초과했을 때 동작이 조금 다름
- CPU 사용량이 Limit을 초과하면 일반적으로 CPU 사용이 제한되는 Throttling이 발생할 수 있음

```
CPU Limit 초과
↓
CPU 사용 제한
↓
응답 속도 저하 가능
```
- Memory는 Limit을 초과하면 Container가 강제로 종료될 수 있음
- `OOMKilled`

```
Memory Limit 512Mi

Application이 700Mi 사용 시도
↓
Memory Limit 초과
↓
OOMKilled
```

## CPU 단위 이해하기
Kubernetes CPU에서 자주 보는 값이 `m`

```
1000m = CPU 1 Core
500m = CPU 0.5 Core
250m = CPU 0.25 Core
```

`cpu: "500m"`은 CPU 절반 정도를 의미

## Memory 단위
Memory에서는 `Mi`, `Gi` 등을 많이 사용

```
256Mi
512Mi
1Gi
2Gi
```

`memory: "512Mi"`라고 설정하면 약 512MiB를 의미

## Spring Boot에서 Memory Limit이 특히 중요한 이유
Java Application에서는 Kubernetes Memory Limit과 JVM Heap 설정을 함께 생각해야 함

```yaml
limits:
  memory: "512Mi"
```
Kubernetes에서 위로 설정했는데 JVM이 너무 많은 Memory를 사용하도로 설정되어 있다면 문제 발생
```
Container Limit
512Mi

JVM Heap
+
Metaspace
+
Thread Stack
+
Direct Memory
+
기타 Memory
```
- 전체 사용량이 Limit을 넘으면 `OOMKilled`가 발생할 수 있음
- 따라서 Pod Memory Limit은 JVM Heap만 고려하면 안 됨

## Resource 설정이 중요한 이유
Resource 설정이 없다면 Scheduler가 해당 Pod가 어느 정도의 Resource를 사용할지 판단하기 어려움

또 하나의 Application이 Resource를 과도하게 사용할 가능성도 있음
```
Node

Application A
CPU 과다 사용
       ↓
Application B 느려짐
Application C 느려짐
```

```
requests

"최소 이 정도는 필요하다."
↓
Scheduler가 Pod 배치할 때 참고
```
```
limits

"최대로 이 정도까지만 사용할 수 있다."
↓
Resource 과도 사용 제한
```
- 단, Limit을 무조건 낮게 설정하는 것도 좋은 것은 아님
- 너무 낮으면 정상적인 Application도 `CPU Throttling`, `OOMKilled` 가능
- 실제 운영에서는 Monitoring 데이터를 기반으로 값을 조정

## Deployment 예시
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

          envFrom:
            - configMapRef:
                name: my-api-config

            - secretRef:
                name: my-api-secret

          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            periodSeconds: 5
            failureThreshold: 12

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5

          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"

            limits:
              cpu: "500m"
              memory: "512Mi"
```

## 실무 명령어
ConfigMap 확인
- `kubectl get configmaps`
- `kubectl describe configmap my-api-config`

Secret 확인
- `kubectl get secrets`
- `kubectl describe secret my-api-secret`

Deployment 재시작
- `kubectl rollout restart deployment/my-api`

배포 상태 확인
- `kubectl rollout status deployment/my-api`

Pod 상세 정보 확인
- `kubectl describe pod <pod-name>`

CPU/Memory 사용량 확인
- `kubectl top pods`

Node Resource 확인
- `kubectl top nodes`

`kubectl top`은 Cluster에 Metrics Server 같은 Metrics 구성요소가 있어야 사용할 수 있음


## 실무 상황 예시

### 1. 운영 DB 주소가 변경
ConfigMap에서 값을 변경
```
ConfigMap 수정
    ↓
Deployment Restart
    ↓
새 Pod에 새로운 환경변수 적용
```

### 2. DB Password를 설정
ConfigMap이 아니라 Secret 사용
```
Secret
    ↓
Environment Variable
    ↓
Spring Boot Datasource
```

### 3. 새 Pod가 올라오자마자 요청을 받아 오류 발생
Spring이 완전히 초기화되기 전에 Service에서 요청이 들어오는 문제일 수 있음

Readiness Probe 확인
```
Pod 생성

Spring Boot 초기화 중

Readiness = False
↓
Service 요청 대상에서 제외
↓
Spring Boot 준비 완료


Readiness = True
↓
Service 요청 전달
```

### 4. Pod가 계속 재시작
- `kubectl get pods`
- `kubectl describe pod <pod-name>`
- `kubectl logs <pod-name>`

원인은 여러가지
```
Application Error
Liveness Probe 실패
Memory Limit 초과
잘못된 ConfigMap / Secret
DB 연결 실패
```

### 5. Pod 상태에 OOMKilled
- `kubectl describe pod <pod-name>`
- `kubectl top pods`
```
Memory Limit이 너무 낮은가?
JVM Heap 설정은 적절한가?
Application Memory Leak이 있는가?
특정 요청에서 Memory를 과도하게 사용하는가?
```
Limit을 단순히 크게 올리는 것보다 원인을 같이 확인

## 핵심 정리

### Kubernetes에서는 Application과 설정을 분리
```
ConfigMap
→ 일반 설정

Secret
→ 민감한 설정
```

### Spring Boot에서는 이를 환경변수로 받아서 사용
```
ConfigMap / Secret
        ↓
Environment Variable
        ↓
Spring Boot
```

### Application 상태 확인에는 Probe를 사용
```
Startup Probe
→ Application 시작 완료 여부

Liveness Probe
→ Application이 살아 있는지

Readiness Probe
→ 요청을 받을 준비가 되었는지
```

### Resource는 Request와 Limit으로 관리
```
requests
→ Pod가 필요로 하는 Resource
→ Scheduling에 사용

limits
→ Pod가 사용할 수 있는 최대 Resource
```
