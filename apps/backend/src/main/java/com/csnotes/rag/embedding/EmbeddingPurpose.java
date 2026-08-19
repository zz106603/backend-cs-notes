package com.csnotes.rag.embedding;

/** 모델이 문서와 질의에 서로 다른 임베딩 방식을 지원할 수 있도록 용도를 구분한다. */
public enum EmbeddingPurpose {
    DOCUMENT,
    QUERY
}
