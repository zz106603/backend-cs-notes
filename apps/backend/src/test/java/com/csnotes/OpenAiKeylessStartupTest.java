package com.csnotes;

import com.csnotes.rag.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.ai.openai.api-key=")
class OpenAiKeylessStartupTest {
    @Autowired
    private ObjectProvider<EmbeddingProvider> embeddingProvider;

    @Test
    void API_키가_없어도_애플리케이션이_기동된다() {
        assertThat(embeddingProvider.getIfAvailable()).isNull();
    }
}
