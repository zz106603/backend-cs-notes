package com.csnotes.rag.embedding;

import java.util.List;

/** 외부 임베딩 API나 Spring AI 구현을 도메인 로직에서 분리하는 포트다. */
public interface EmbeddingProvider {
    String modelName();

    int dimensions();

    List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose);

    /** 저장할 Chunk를 문서용 입력으로 일괄 임베딩한다. */
    default List<EmbeddingVector> embedDocuments(List<EmbeddingInput> inputs) {
        return validate(embed(inputs, EmbeddingPurpose.DOCUMENT), inputs);
    }

    /** 검색어를 질의용 입력으로 임베딩한다. */
    default EmbeddingVector embedQuery(String query) {
        EmbeddingInput input = new EmbeddingInput("query", query);
        return validate(embed(List.of(input), EmbeddingPurpose.QUERY), List.of(input)).getFirst();
    }

    /** 제공자가 입력 순서, 모델 또는 차원을 바꾸면 벡터 저장 전에 실패시킨다. */
    private List<EmbeddingVector> validate(List<EmbeddingVector> vectors, List<EmbeddingInput> inputs) {
        if (vectors == null || vectors.size() != inputs.size()) {
            throw new IllegalStateException("Embedding provider returned an unexpected number of vectors");
        }
        for (int index = 0; index < vectors.size(); index++) {
            EmbeddingVector vector = vectors.get(index);
            if (!vector.inputId().equals(inputs.get(index).id())) {
                throw new IllegalStateException("Embedding provider changed input order");
            }
            if (!vector.model().equals(modelName()) || vector.dimensions() != dimensions()) {
                throw new IllegalStateException("Embedding provider returned incompatible vector metadata");
            }
        }
        return List.copyOf(vectors);
    }
}
