package com.csnotes.rag.reranking.cohere;

import com.csnotes.rag.reranking.ChunkReranker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled", "rag.reranking.enabled"}, havingValue = "true")
public class CohereRerankerConfiguration {
    @Bean
    @Conditional(CohereApiKeyCondition.class)
    ChunkReranker cohereChunkReranker(
            ObjectMapper objectMapper,
            @Value("${rag.reranking.cohere.api-key}") String apiKey,
            @Value("${rag.reranking.cohere.base-url}") String baseUrl,
            @Value("${rag.reranking.cohere.model}") String model,
            @Value("${rag.reranking.cohere.max-tokens-per-document}") int maxTokensPerDocument,
            @Value("${rag.reranking.cohere.connect-timeout}") Duration connectTimeout,
            @Value("${rag.reranking.cohere.read-timeout}") Duration readTimeout,
            @Value("${rag.reranking.cohere.cache-ttl}") Duration cacheTtl,
            @Value("${rag.reranking.cohere.cache-max-entries}") int cacheMaxEntries,
            @Value("${rag.reranking.cohere.max-requests-per-minute}") int maxRequestsPerMinute
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("X-Client-Name", "backend-cs-notes")
                .build();
        return new CohereChunkReranker(restClient, objectMapper, model, maxTokensPerDocument,
                cacheTtl, cacheMaxEntries, maxRequestsPerMinute);
    }
}
