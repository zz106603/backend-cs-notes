package com.csnotes.rag.search;

import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.persistence.ChunkVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled"}, havingValue = "true")
public class RagSearchConfiguration {
    @Bean
    RagSearchService ragSearchService(
            ObjectProvider<EmbeddingProvider> embeddingProvider,
            ChunkVectorStore vectorStore,
            @Value("${rag.search.default-limit}") int defaultLimit,
            @Value("${rag.search.max-limit}") int maxLimit,
            @Value("${rag.search.max-query-characters}") int maxQueryCharacters,
            @Value("${rag.search.default-minimum-score}") double defaultMinimumScore,
            @Value("${rag.search.sparse-default-minimum-score}") double sparseDefaultMinimumScore,
            @Value("${rag.search.hybrid-candidate-limit}") int hybridCandidateLimit,
            @Value("${rag.search.hybrid-rrf-k}") int hybridRrfK,
            @Value("${rag.search.query-cache-ttl}") Duration cacheTtl,
            @Value("${rag.search.query-cache-max-entries}") int cacheMaxEntries
    ) {
        return new RagSearchService(embeddingProvider.getIfAvailable(), vectorStore, defaultLimit, maxLimit,
                maxQueryCharacters, defaultMinimumScore, sparseDefaultMinimumScore,
                hybridCandidateLimit, hybridRrfK, cacheTtl, cacheMaxEntries);
    }
}
