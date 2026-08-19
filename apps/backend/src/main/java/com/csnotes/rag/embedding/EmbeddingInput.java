package com.csnotes.rag.embedding;

/** 임베딩 결과를 원래 Chunk나 질의와 연결하기 위한 입력 모델이다. */
public record EmbeddingInput(String id, String text) {
    public EmbeddingInput {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Embedding input id is required");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Embedding input text is required");
    }
}
