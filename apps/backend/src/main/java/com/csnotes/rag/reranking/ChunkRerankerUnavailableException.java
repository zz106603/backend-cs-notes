package com.csnotes.rag.reranking;

/** 외부 Reranker를 일시적으로 사용할 수 없어 기존 RRF 결과로 복귀할 수 있음을 나타낸다. */
public final class ChunkRerankerUnavailableException extends RuntimeException {
    private final String reason;

    public ChunkRerankerUnavailableException(String reason, Throwable cause) {
        super("External reranker is unavailable: " + reason, cause);
        this.reason = reason;
    }

    public ChunkRerankerUnavailableException(String reason) {
        this(reason, null);
    }

    public String reason() {
        return reason;
    }
}
