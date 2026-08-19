package com.csnotes;

import com.csnotes.rag.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("openai-live")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*\\S.*")
@SpringBootTest
class OpenAiEmbeddingLiveTest {
    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Test
    void 실제_OpenAI_API에서_1536차원_임베딩을_반환한다() {
        var vector = embeddingProvider.embedQuery("OpenAI 임베딩 연결 확인");

        assertThat(vector.model()).isEqualTo("text-embedding-3-small");
        assertThat(vector.values()).hasSize(1536);
    }
}
