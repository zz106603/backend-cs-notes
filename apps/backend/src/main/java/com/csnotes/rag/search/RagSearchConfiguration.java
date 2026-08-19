package com.csnotes.rag.search;

import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.OpenAiApiKeyCondition;
import com.csnotes.rag.persistence.ChunkVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Conditional(OpenAiApiKeyCondition.class)
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled"}, havingValue = "true")
public class RagSearchConfiguration {
    @Bean
    RagSearchService ragSearchService(
            EmbeddingProvider embeddingProvider,
            ChunkVectorStore vectorStore,
            @Value("${rag.search.default-limit}") int defaultLimit,
            @Value("${rag.search.max-limit}") int maxLimit,
            @Value("${rag.search.max-query-characters}") int maxQueryCharacters,
            @Value("${rag.search.default-minimum-score}") double defaultMinimumScore,
            @Value("${rag.search.query-cache-ttl}") Duration cacheTtl,
            @Value("${rag.search.query-cache-max-entries}") int cacheMaxEntries
    ) {
        return new RagSearchService(embeddingProvider, vectorStore, defaultLimit, maxLimit,
                maxQueryCharacters, defaultMinimumScore, cacheTtl, cacheMaxEntries);
    }
}
