package com.csnotes.rag.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiEmbeddingProviderTest {
    @Test
    void 스프링_AI_응답을_도메인_임베딩_벡터로_변환한다() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        EmbeddingResponse response = mock(EmbeddingResponse.class);
        org.springframework.ai.embedding.Embedding embedding = mock(org.springframework.ai.embedding.Embedding.class);
        when(model.embedForResponse(List.of("문서 본문"))).thenReturn(response);
        when(response.getResults()).thenReturn(List.of(embedding));
        when(embedding.getOutput()).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        var provider = new SpringAiEmbeddingProvider(model, "test-model", 3);

        var vectors = provider.embedDocuments(List.of(new EmbeddingInput("chunk-1", "문서 본문")));

        assertThat(vectors.getFirst().inputId()).isEqualTo("chunk-1");
        assertThat(vectors.getFirst().model()).isEqualTo("test-model");
        assertThat(vectors.getFirst().values()).containsExactly(0.1f, 0.2f, 0.3f);
    }
}
