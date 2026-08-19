package com.csnotes.rag.embedding;

import java.util.Arrays;

/** 모델명과 벡터 차원을 함께 보존해 호환되지 않는 벡터 저장을 방지한다. */
public record EmbeddingVector(String inputId, String model, float[] values) {
    public EmbeddingVector {
        if (inputId == null || inputId.isBlank()) throw new IllegalArgumentException("Embedding input id is required");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("Embedding model is required");
        if (values == null || values.length == 0) throw new IllegalArgumentException("Embedding vector is required");
        values = Arrays.copyOf(values, values.length);
    }

    /** 외부에서 배열을 수정해 저장된 임베딩이 바뀌지 않도록 복사본을 반환한다. */
    @Override
    public float[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public int dimensions() {
        return values.length;
    }
}
