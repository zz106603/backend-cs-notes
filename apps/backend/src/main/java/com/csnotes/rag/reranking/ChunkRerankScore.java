package com.csnotes.rag.reranking;

/** 모델 구현체가 반환하는 Chunk별 절대 관련성 점수다. */
public record ChunkRerankScore(String chunkId, double relevanceScore) {
}
