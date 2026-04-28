# Docker (컨테이너 가상화 기술)

애플리케이션을 실행하는 데 필요한 모든 환경(코드, 라이브러리, 설정 등)을 하나로 묶어 어디서나 동일하게 실행할 수 있도록 돕는 **컨테이너 가상화 플랫폼**

---

## 1. 핵심 개념: 이미지와 컨테이너

### 1) 이미지 (Image)
*   애플리케이션 실행에 필요한 모든 파일과 설정이 포함된 **읽기 전용 템플릿**
*   붕어빵 틀에 비유할 수 있으며, 한 번 빌드된 이미지는 변하지 않음(Immutable)

### 2) 컨테이너 (Container)
*   이미지를 실행한 **독립적인 프로세스 인스턴스**
*   붕어빵 틀(이미지)에서 찍어낸 붕어빵(컨테이너)과 같으며, 독립된 파일 시스템과 네트워크를 가짐

---

## 2. Docker가 해결하는 실무 문제

### 1) "내 컴퓨터에선 되는데 서버에선 안 돼요" (환경 일치)
*   **문제**: 개발 환경(Windows), 테스트 환경(Linux), 운영 환경(Cloud)의 OS 버전이나 설치된 라이브러리가 달라 에러가 발생하는 경우가 빈번함
*   **해결**: Docker 이미지를 사용하면 개발 환경과 서버 환경을 100% 일치시킬 수 있음

### 2) 가볍고 빠른 배포 (컨테이너 vs VM)
*   **VM (Virtual Machine)**: 하드웨어 전체를 가상화하여 게스트 OS를 통째로 올리므로 무겁고 부팅이 느림
*   **Docker (Container)**: 호스트의 커널을 공유하며 프로세스 단위로 격리되므로 크기가 작고 실행 속도가 밀리초(ms) 단위로 매우 빠름

---

## 3. Docker 핵심 요소 및 실무 예시

### 1) Dockerfile (이미지 설계도)
이미지를 어떻게 만들지 정의하는 텍스트 파일

```dockerfile
# 1. 베이스 이미지 설정 (Java 환경)
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 생성
WORKDIR /app

# 3. 빌드된 jar 파일을 컨테이너 내부로 복사
COPY build/libs/myapp.jar app.jar

# 4. 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2) Docker Compose (멀티 컨테이너 관리)
여러 컨테이너(애플리케이션, DB, Redis 등)를 한꺼번에 정의하고 실행할 때 사용함

```yaml
# docker-compose.yml
services:
  web:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
  db:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: password
    volumes: # 데이터 영속성 설정
      - db_data:/var/lib/postgresql/data
volumes:
  db_data: # 볼륨 정의
```

### 3) Docker Networking (컨테이너 간 통신)
컨테이너들이 서로 통신하거나 외부와 통신하는 방식을 정의함

*   **Bridge Network (기본값)**: 동일한 브릿지 네트워크에 연결된 컨테이너들은 서로 이름으로 통신할 수 있음
    *   **실무 예시**: `docker-compose`로 실행된 `web` 컨테이너가 `db` 컨테이너에 `jdbc:postgresql://db:5432/mydb`와 같이 서비스 이름으로 접근 가능

### 4) Docker Volumes (데이터 영속성)
컨테이너가 삭제되어도 데이터가 사라지지 않도록 호스트 머신에 데이터를 저장하는 방법

*   **Bind Mounts**: 호스트 파일 시스템의 특정 경로를 컨테이너에 마운트 함 (개발 시 소스 코드 공유에 유용)
*   **Volumes (추천)**: Docker가 관리하는 호스트 파일 시스템의 영역에 데이터를 저장함 (DB 데이터, 로그 파일 등 영속적인 데이터 저장에 적합)
    *   **실무 예시**: PostgreSQL 컨테이너의 `/var/lib/postgresql/data` 경로를 `db_data` 볼륨에 연결하여 컨테이너가 재생성되어도 DB 데이터가 유지되도록 함

---

## 4. Dockerfile Best Practices (효율적이고 안전한 이미지 빌드)

1.  **작은 베이스 이미지 사용**: `alpine` 버전이나 `slim` 버전을 사용하여 이미지 크기를 최소화 함 (예: `openjdk:17-jdk-slim`)
2.  **멀티 스테이지 빌드 (Multi-stage Builds)**: 빌드 환경과 런타임 환경을 분리하여 최종 이미지에 불필요한 빌드 도구들을 포함하지 않음
    ```dockerfile
    # 빌드 스테이지
    FROM gradle:jdk17 AS builder
    WORKDIR /app
    COPY . .
    RUN gradle clean build -x test

    # 런타임 스테이지
    FROM openjdk:17-jdk-slim
    WORKDIR /app
    COPY --from=builder /app/build/libs/*.jar app.jar
    ENTRYPOINT ["java", "-jar", "app.jar"]
    ```
3.  **캐시 활용**: 자주 변경되지 않는 레이어를 먼저 배치하여 Docker 빌드 캐시를 효율적으로 사용함 (예: `COPY . .` 전에 `COPY build.gradle .` 등)
4.  **불필요한 파일 제외**: `.dockerignore` 파일을 사용하여 빌드 시 불필요한 파일(예: `.git`, `target/`, `node_modules`)이 이미지에 포함되지 않도록 함

---

## 5. 실무 활용 시나리오

1.  **신규 팀원 온보딩**: `docker-compose up` 명령어 하나로 DB, Redis, RabbitMQ 등 모든 인프라를 한 번에 구축하여 즉시 개발을 시작할 수 있음
2.  **CI/CD 파이프라인**: Jenkins나 GitHub Actions에서 코드를 빌드한 후 Docker 이미지를 만들어 레지스트리(Docker Hub, ECR)에 푸시하고, 운영 서버는 이 이미지를 pull 받아 실행만 하면 배포가 끝남
3.  **마이크로서비스(MSA)**: 각 서비스마다 서로 다른 언어(Java, Python, Go)와 버전을 사용하더라도 컨테이너로 격리하여 충돌 없이 운영할 수 있음

---

## 6. 요약 및 결론

| 구분 | 가상 머신 (VM) | 도커 컨테이너 (Docker) |
| :--- | :--- | :--- |
| **격리 수준** | 하드웨어 수준 (완벽한 격리) | 프로세스 수준 (OS 커널 공유) |
| **속도** | 느림 (OS 부팅 필요) | 매우 빠름 (프로세스 실행) |
| **자원 효율** | 낮음 (메모리 미리 점유) | 높음 (필요한 만큼 사용) |

### 결론
> **Docker는 현대 백엔드 엔지니어에게 "배포의 표준"임.**
> 단순한 유틸리티를 넘어 서버 인프라를 코드로 관리(IaC)하고, 확장성 있는 시스템을 구축하기 위한 필수 기술
