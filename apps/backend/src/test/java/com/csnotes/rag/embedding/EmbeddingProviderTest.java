package com.csnotes.rag.embedding;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingProviderTest {
    @Test
    void 문서와_질의_임베딩_목적을_구분한다() {
        var provider = new RecordingEmbeddingProvider();

        var documents = provider.embedDocuments(List.of(new EmbeddingInput("chunk-1", "문서 본문")));
        var query = provider.embedQuery("트랜잭션이란?");

        assertThat(provider.purposes).containsExactly(EmbeddingPurpose.DOCUMENT, EmbeddingPurpose.QUERY);
        assertThat(documents.getFirst().inputId()).isEqualTo("chunk-1");
        assertThat(query.inputId()).isEqualTo("query");
        assertThat(query.dimensions()).isEqualTo(3);
    }

    @Test
    void 예상과_다른_차원의_벡터를_거부한다() {
        EmbeddingProvider provider = new RecordingEmbeddingProvider() {
            @Override
            public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
                return List.of(new EmbeddingVector(inputs.getFirst().id(), modelName(), new float[]{1, 2}));
            }
        };

        assertThatThrownBy(() -> provider.embedQuery("질문"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incompatible");
    }

    private static class RecordingEmbeddingProvider implements EmbeddingProvider {
        private final List<EmbeddingPurpose> purposes = new ArrayList<>();

        @Override
        public String modelName() {
            return "test-embedding";
        }

        @Override
        public int dimensions() {
            return 3;
        }

        @Override
        public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
            purposes.add(purpose);
            return inputs.stream()
                    .map(input -> new EmbeddingVector(input.id(), modelName(), new float[]{1, 0, purpose.ordinal()}))
                    .toList();
        }
    }
}
