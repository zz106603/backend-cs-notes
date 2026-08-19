package com.csnotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// RAG DB는 rag.persistence.enabled 조건에 따라 직접 구성하므로 기본 DataSource 자동 구성을 사용하지 않는다.
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class CsNotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsNotesApplication.class, args);
    }
}
