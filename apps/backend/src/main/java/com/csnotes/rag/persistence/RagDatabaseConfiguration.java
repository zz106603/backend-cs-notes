package com.csnotes.rag.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "rag.persistence.enabled", havingValue = "true")
public class RagDatabaseConfiguration {
    @Bean(destroyMethod = "close")
    DataSource ragDataSource(
            @Value("${rag.persistence.url}") String url,
            @Value("${rag.persistence.username}") String username,
            @Value("${rag.persistence.password}") String password
    ) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("rag-pgvector");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    @Bean(initMethod = "migrate")
    Flyway ragFlyway(DataSource ragDataSource) {
        return Flyway.configure().dataSource(ragDataSource).locations("classpath:db/migration").load();
    }

    @Bean
    JdbcTemplate ragJdbcTemplate(DataSource ragDataSource) {
        return new JdbcTemplate(ragDataSource);
    }

    @Bean
    PlatformTransactionManager ragTransactionManager(DataSource ragDataSource) {
        return new DataSourceTransactionManager(ragDataSource);
    }
}
