package com.csnotes.rag.search;

import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.reranking.ChunkReranker;
import com.csnotes.rag.reranking.RagRerankingService;
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
            @Value("${rag.reranking.enabled}") boolean rerankingEnabled,
            @Value("${rag.reranking.minimum-score}") double rerankingMinimumScore,
            ObjectProvider<ChunkReranker> chunkReranker,
            @Value("${rag.search.query-cache-ttl}") Duration cacheTtl,
            @Value("${rag.search.query-cache-max-entries}") int cacheMaxEntries
    ) {
        // 기본값은 비활성이다. 나중에 ChunkReranker Bean만 교체하면 검색 서비스 변경 없이 모델을 연결할 수 있다.
        RagRerankingService rerankingService = new RagRerankingService(
                rerankingEnabled, chunkReranker.getIfAvailable(), rerankingMinimumScore);
        return new RagSearchService(embeddingProvider.getIfAvailable(), vectorStore, defaultLimit, maxLimit,
                maxQueryCharacters, defaultMinimumScore, sparseDefaultMinimumScore,
                hybridCandidateLimit, hybridRrfK, rerankingService, cacheTtl, cacheMaxEntries);
    }
}
