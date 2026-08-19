package com.csnotes;

import com.csnotes.rag.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class OpenAiConfiguredStartupTest {
    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Test
    void API_키가_있으면_OpenAI_임베딩_제공자를_구성한다() {
        assertThat(embeddingProvider.modelName()).isEqualTo("text-embedding-3-small");
        assertThat(embeddingProvider.dimensions()).isEqualTo(1536);
    }
}
