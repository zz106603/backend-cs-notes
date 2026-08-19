package com.csnotes.rag.indexing;

/** 예상보다 큰 색인 요청이 OpenAI 비용으로 이어지기 전에 중단됐음을 나타낸다. */
public class RagIndexingLimitException extends RuntimeException {
    public RagIndexingLimitException(String message) {
        super(message);
    }
}
