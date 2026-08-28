package com.csnotes.rag.evaluation;

import com.csnotes.rag.search.RagSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled", "rag.evaluation.enabled"}, havingValue = "true")
public class RagEvaluationConfiguration {
    @Bean
    RagEvaluationRepository ragEvaluationRepository(JdbcTemplate ragJdbcTemplate) {
        return new JdbcRagEvaluationRepository(ragJdbcTemplate);
    }

    @Bean
    RagEvaluationService ragEvaluationService(
            RagEvaluationRepository repository,
            RagSearchService searchService,
            @Value("${rag.evaluation.result-limit}") int resultLimit
    ) {
        return new RagEvaluationService(repository, searchService, resultLimit, Clock.systemUTC());
    }
}
