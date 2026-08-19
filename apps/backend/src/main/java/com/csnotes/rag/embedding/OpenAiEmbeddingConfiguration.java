package com.csnotes.rag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;

@Configuration
public class OpenAiEmbeddingConfiguration {
    /** 키가 있는 경우에만 OpenAI 클라이언트와 임베딩 모델을 구성해 로컬 개발의 선택권을 보존한다. */
    @Bean
    @Conditional(OpenAiApiKeyCondition.class)
    @ConditionalOnMissingBean(EmbeddingModel.class)
    EmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${rag.embedding.model}") String modelName,
            @Value("${rag.embedding.dimensions}") int dimensions,
            @Value("${spring.ai.retry.max-attempts:3}") int maxAttempts,
            @Value("${spring.ai.retry.backoff.initial-interval:1s}") Duration initialInterval,
            @Value("${spring.ai.retry.backoff.multiplier:2}") double multiplier,
            @Value("${spring.ai.retry.backoff.max-interval:10s}") Duration maxInterval
    ) {
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelName)
                .dimensions(dimensions)
                .build();
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .exponentialBackoff(initialInterval, multiplier, maxInterval)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options, retryTemplate);
    }

    /** Spring AI 모델을 도메인 포트로 감싸 색인·검색 로직이 외부 SDK에 직접 의존하지 않게 한다. */
    @Bean
    @Conditional(OpenAiApiKeyCondition.class)
    EmbeddingProvider openAiEmbeddingProvider(
            EmbeddingModel embeddingModel,
            @Value("${rag.embedding.model}") String modelName,
            @Value("${rag.embedding.dimensions}") int dimensions,
            @Value("${rag.embedding.batch-size}") int batchSize
    ) {
        return new SpringAiEmbeddingProvider(embeddingModel, modelName, dimensions, batchSize);
    }
}
