package com.csnotes.rag.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void 설정한_크기로_나누어_입력_순서대로_임베딩한다() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        EmbeddingResponse firstResponse = responseWith(new float[]{1f}, new float[]{2f});
        EmbeddingResponse secondResponse = responseWith(new float[]{3f});
        when(model.embedForResponse(List.of("첫 번째", "두 번째")))
                .thenReturn(firstResponse);
        when(model.embedForResponse(List.of("세 번째")))
                .thenReturn(secondResponse);
        var provider = new SpringAiEmbeddingProvider(model, "test-model", 1, 2);

        var vectors = provider.embedDocuments(List.of(
                new EmbeddingInput("chunk-1", "첫 번째"),
                new EmbeddingInput("chunk-2", "두 번째"),
                new EmbeddingInput("chunk-3", "세 번째")
        ));

        assertThat(vectors).extracting(EmbeddingVector::inputId)
                .containsExactly("chunk-1", "chunk-2", "chunk-3");
        verify(model).embedForResponse(List.of("첫 번째", "두 번째"));
        verify(model).embedForResponse(List.of("세 번째"));
    }

    @Test
    void 외부_모델_오류를_임베딩_제공자_예외로_변환한다() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embedForResponse(List.of("문서 본문"))).thenThrow(new IllegalStateException("외부 오류"));
        var provider = new SpringAiEmbeddingProvider(model, "test-model", 3);

        assertThatThrownBy(() -> provider.embedDocuments(List.of(new EmbeddingInput("chunk-1", "문서 본문"))))
                .isInstanceOf(EmbeddingProviderException.class)
                .hasMessage("Embedding API request failed")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private EmbeddingResponse responseWith(float[]... outputs) {
        EmbeddingResponse response = mock(EmbeddingResponse.class);
        var embeddings = java.util.Arrays.stream(outputs).map(output -> {
            org.springframework.ai.embedding.Embedding embedding = mock(org.springframework.ai.embedding.Embedding.class);
            when(embedding.getOutput()).thenReturn(output);
            return embedding;
        }).toList();
        when(response.getResults()).thenReturn(embeddings);
        return response;
    }
}
