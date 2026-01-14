## CI/CD 파이프라인 (Continuous Integration / Continuous Delivery & Deployment)

### 1. 개념 요약
소프트웨어 개발 단계부터 배포까지의 과정을 자동화하여, **더 자주, 더 빠르고, 더 안정적으로** 사용자에게 서비스를 제공하는 방법론

---

### 2. CI (Continuous Integration, 지속적 통합)
개발자들이 작성한 코드를 **하루에도 여러 번 중앙 저장소(Main Branch)에 통합**하고, **자동화된 빌드와 테스트**를 수행하는 과정

- **핵심 활동**:
  - **코드 통합**: 개발자가 작업한 코드를 Git 등에 Push
  - **자동 빌드**: 코드가 통합될 때마다 자동으로 빌드 수행
  - **자동 테스트**: 단위 테스트, 통합 테스트 등을 수행하여 코드 품질 검증
- **목표**:
  - 버그를 조기에 발견하고 수정 (Fail Fast)
  - 통합 지옥(Integration Hell) 방지
  - 항상 배포 가능한 상태의 코드 베이스 유지

### 3. CD (Continuous Delivery & Deployment)
CI 과정을 통과한 코드를 실제 사용자 환경(Production)까지 자동으로 릴리스하는 과정. CD는 두 가지 의미로 나뉨

#### 1) 지속적 전달 (Continuous Delivery)
- CI를 통과한 빌드 결과물(Artifact)을 **운영 환경에 배포할 준비가 된 상태**로 만듬
- **특징**: 실제 배포는 **수동(Manual Approval)** 으로 이루어짐 (예: "배포 버튼"을 관리자가 눌러야 배포됨)
- **목적**: 비즈니스 결정에 따라 언제든지 배포할 수 있는 상태 유지

#### 2) 지속적 배포 (Continuous Deployment)
- CI/CD 파이프라인의 모든 단계를 통과하면 **사람의 개입 없이 자동으로 운영 환경에 배포**됨
- **특징**: 완전 자동화
- **목적**: 개발된 기능을 사용자에게 가장 빠르게 전달

---

### 4. CI/CD 파이프라인 흐름 (실무 예시)

일반적인 웹 애플리케이션 개발 환경에서의 CI/CD 흐름

```text
[개발자] -> [Git Push] -> [CI 도구] -> [빌드/테스트] -> [이미지 생성] -> [CD 도구] -> [서버 배포]
```

#### 1단계: 코드 작성 및 푸시 (Code)
- 개발자가 기능을 구현하고 GitHub의 `develop` 브랜치에 코드를 Push 함

#### 2단계: CI 프로세스 (Build & Test) - Github Actions / Jenkins
- **Trigger**: Push 이벤트를 감지하여 CI 도구가 동작
- **Build**: Gradle/Maven 등을 사용하여 프로젝트를 빌드
- **Test**: JUnit 등을 사용하여 단위 테스트를 실행. 실패 시 개발자에게 알림(Slack 등)을 보냅니다.
- **Artifact**: 빌드가 성공하면 실행 가능한 파일(JAR)이나 Docker 이미지를 생성

#### 3단계: CD 프로세스 (Deploy)
- **이미지 저장**: 생성된 Docker 이미지를 저장소(AWS ECR, Docker Hub)에 업로드
- **배포**:
  - **Dev 환경**: 자동으로 배포하여 QA 진행
  - **Prod 환경**: (지속적 전달의 경우) 승인 절차 후 배포, (지속적 배포의 경우) 즉시 배포
  - AWS CodeDeploy, ArgoCD 등을 사용하여 새로운 버전의 컨테이너를 실행

---

### 5. 대표적인 도구 (Tools)
- **CI/CD 통합**: Jenkins, GitHub Actions, GitLab CI
- **빌드 도구**: Gradle, Maven, Npm
- **컨테이너 및 오케스트레이션**: Docker, Kubernetes
- **클라우드 배포**: AWS CodePipeline, AWS CodeDeploy

### 6. 도입 효과
- **개발 생산성 향상**: 반복적인 빌드/배포 작업 제거
- **안정성 증대**: 사람의 실수(Human Error) 방지 및 일관된 배포 환경 보장
- **빠른 피드백**: 문제 발생 시 즉각적인 알림으로 빠른 대응 가능