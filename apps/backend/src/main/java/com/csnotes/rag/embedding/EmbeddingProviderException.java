package com.csnotes.rag.embedding;

/** 외부 임베딩 서비스 호출 또는 응답 변환 실패를 애플리케이션 경계에서 표현한다. */
public class EmbeddingProviderException extends RuntimeException {
    public EmbeddingProviderException(String message) {
        super(message);
    }

    public EmbeddingProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
