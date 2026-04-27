# Gradle (빌드 자동화 시스템)

프로젝트에 필요한 **의존성(라이브러리)을 관리**하고, 소스 코드를 **컴파일, 테스트, 패키징(빌드)** 하는 과정을 자동화해주는 도구

---

## 1. 왜 필요한가요?

### 문제 상황: Gradle이 없다면?
1.  **의존성 지옥**: Spring Boot 웹 프로젝트를 하려면 `spring-web`, `tomcat`, `jackson` 등 수십 개의 `.jar` 파일을 직접 다운로드해서 프로젝트에 추가해야 함. 버전이 서로 안 맞으면 충돌이 일어남
2.  **반복적인 빌드 작업**: 코드를 수정한 뒤, 매번 수동으로 컴파일하고, 테스트 코드를 실행하고, `.jar` 또는 `.war` 파일로 압축해야 함. 매우 귀찮고 실수하기 쉬움
3.  **환경 불일치**: 개발자 A의 PC에서는 빌드가 되는데, 개발자 B의 PC에서는 자바 버전이나 라이브러리 경로가 달라서 빌드가 실패함

### Gradle의 해결책
*   **의존성 관리**: `build.gradle` 파일에 `implementation 'org.springframework.boot:spring-boot-starter-web'` 한 줄만 추가하면, 필요한 모든 라이브러리를 알아서 다운로드하고 관리해줌
*   **빌드 자동화**: `gradlew build` 명령어 하나만 실행하면 **컴파일 → 테스트 → 패키징**까지 모든 과정을 자동으로 수행해 줌
*   **환경 통일**: **Gradle Wrapper (`gradlew`)** 를 통해 모든 개발자와 CI/CD 서버가 동일한 버전의 Gradle을 사용하여 빌드하도록 강제하여, 환경 불일치 문제를 해결함

---

## 2. 핵심 개념 및 `build.gradle` 분석

### 1) `build.gradle` (또는 `build.gradle.kts`)
프로젝트의 "설계도"와 같은 파일. Groovy(구) 또는 Kotlin(신) 언어로 작성됨

```groovy
// build.gradle 예시 (Spring Boot)

// 1. 플러그인 설정
plugins {
    id 'java'
    id 'org.springframework.boot' version '2.7.17'
    id 'io.spring.dependency-management' version '1.0.15.RELEASE'
}

// 2. 기본 정보
group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17' // 자바 버전

// 3. 라이브러리 다운로드 경로
repositories {
    mavenCentral()
}

// 4. 의존성(라이브러리) 목록
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.h2database:h2'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// 5. 태스크(작업) 정의
tasks.named('test') {
    useJUnitPlatform()
}
```
*   **`plugins`**: 프로젝트에 필요한 기능들을 확장함 (예: Spring Boot 플러그인, Java 플러그인)
*   **`repositories`**: `dependencies`에 선언된 라이브러리를 어디서 다운로드할지 지정함. `mavenCentral()`은 공개 라이브러리 저장소
*   **`dependencies`**: 프로젝트에서 사용할 라이브러리를 선언함
    *   `implementation`: 구현 시에만 필요한 라이브러리. 가장 일반적으로 사용됨
    *   `testImplementation`: 테스트 코드에서만 사용되는 라이브러리 (예: JUnit)
    *   `runtimeOnly`: 실행 시에만 필요한 라이브러리 (예: H2 데이터베이스 드라이버)

### 2) Tasks (태스크)
Gradle이 수행하는 모든 작업의 단위 (예: `compileJava`, `test`, `jar`, `build`, `clean`)
*   `gradlew build`: `build` 태스크를 실행함. 이 태스크는 다른 여러 태스크(컴파일, 테스트 등)에 의존하므로, 관련된 모든 작업이 순서대로 실행됨
*   `gradlew clean`: `build` 폴더(빌드 결과물)를 삭제함

### 3) Gradle Wrapper (`gradlew`, `gradlew.bat`)
*   로컬에 Gradle을 설치하지 않아도, 해당 프로젝트에 맞는 Gradle 버전을 자동으로 다운로드하여 빌드를 실행해주는 스크립트
*   **실무에서는 `gradle` 명령어가 아닌 `gradlew` 명령어를 사용하는 것이 원칙**

---

## 3. Gradle vs. Maven

| 구분 | **Gradle** | **Maven** |
| :--- | :--- | :--- |
| **설정 파일** | **Groovy/Kotlin DSL** (`build.gradle`) | **XML** (`pom.xml`) |
| **유연성** | **높음** (코드로 빌드 로직 작성 가능) | **낮음** (정해진 규칙을 따라야 함) |
| **성능** | **빠름** (증분 빌드, 빌드 캐시) | 상대적으로 느림 |
| **진입 장벽** | 상대적으로 높음 | 상대적으로 낮음 |

**결론**: 최근에는 유연하고 성능이 좋은 **Gradle**이 Spring Boot를 포함한 대부분의 신규 프로젝트에서 표준처럼 사용되고 있음
