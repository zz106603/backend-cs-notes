package com.csnotes.rag.reranking.cohere;

import com.csnotes.rag.reranking.ChunkReranker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;

import static org.assertj.core.api.Assertions.assertThat;

class CohereRerankerConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(CohereRerankerConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues(
                    "rag.persistence.enabled=true",
                    "rag.search.enabled=true",
                    "rag.reranking.enabled=true",
                    "rag.reranking.cohere.base-url=https://api.cohere.test",
                    "rag.reranking.cohere.model=rerank-v4.0-fast",
                    "rag.reranking.cohere.max-tokens-per-document=1024",
                    "rag.reranking.cohere.connect-timeout=2s",
                    "rag.reranking.cohere.read-timeout=5s",
                    "rag.reranking.cohere.cache-ttl=10m",
                    "rag.reranking.cohere.cache-max-entries=100",
                    "rag.reranking.cohere.max-requests-per-minute=8"
            );

    @Test
    void 활성화해도_API_키가_없으면_Cohere_구현체를_만들지_않는다() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ChunkReranker.class));
    }

    @Test
    void 활성화하고_API_키가_있으면_Cohere_구현체를_만든다() {
        contextRunner.withPropertyValues("rag.reranking.cohere.api-key=test-key")
                .run(context -> assertThat(context).hasSingleBean(ChunkReranker.class));
    }
}
