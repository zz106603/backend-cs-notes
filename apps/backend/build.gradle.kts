plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.csnotes"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.8"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-jdbc")
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("openai-live", "rag-search-live", "rag-answer-live", "cohere-live")
    }
}

tasks.register<Test>("cohereRerankerLiveTest") {
    description = "실제 Cohere Trial API로 한국어 Chunk 재정렬을 확인합니다."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("cohere-live")
    }
    onlyIf("COHERE_API_KEY 환경 변수가 설정되어 있어야 합니다.") {
        !System.getenv("COHERE_API_KEY").isNullOrBlank()
    }
}

tasks.register<Test>("ragSearchLiveTest") {
    description = "실제 OpenAI 질의 임베딩과 로컬 pgvector 유사도 검색을 확인합니다."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("rag-search-live")
    }
    onlyIf("OPENAI_API_KEY 환경 변수와 실행 중인 로컬 PostgreSQL이 필요합니다.") {
        !System.getenv("OPENAI_API_KEY").isNullOrBlank()
    }
}

tasks.register<Test>("ragAnswerLiveTest") {
    description = "실제 OpenAI와 로컬 pgvector로 근거 기반 RAG 답변을 확인합니다."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("rag-answer-live")
    }
    onlyIf("OPENAI_API_KEY 환경 변수와 색인된 로컬 PostgreSQL이 필요합니다.") {
        !System.getenv("OPENAI_API_KEY").isNullOrBlank()
    }
}

tasks.register<Test>("openAiLiveTest") {
    description = "실제 OpenAI API로 임베딩 연결을 확인합니다."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("openai-live")
    }
    onlyIf("OPENAI_API_KEY 환경 변수가 설정되어 있어야 합니다.") {
        !System.getenv("OPENAI_API_KEY").isNullOrBlank()
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}
