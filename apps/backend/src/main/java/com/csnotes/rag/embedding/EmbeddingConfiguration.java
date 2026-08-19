package com.csnotes.rag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfiguration {
    /** 실제 EmbeddingModel이 설정된 환경에서만 프레임워크 어댑터를 활성화한다. */
    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    EmbeddingProvider springAiEmbeddingProvider(
            EmbeddingModel embeddingModel,
            @Value("${rag.embedding.model}") String modelName,
            @Value("${rag.embedding.dimensions}") int dimensions
    ) {
        return new SpringAiEmbeddingProvider(embeddingModel, modelName, dimensions);
    }
}
