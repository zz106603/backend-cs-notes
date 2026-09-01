package com.csnotes.rag.reranking;

import java.util.List;

/**
 * 질문과 검색 후보를 직접 비교해 모델 고유의 관련성 점수를 반환하는 교체 지점이다.
 * 외부 API나 로컬 모델 구현체는 이 인터페이스만 구현하며 검색·RRF 로직에는 의존하지 않는다.
 */
public interface ChunkReranker {
    /** 로그와 응답 메타데이터에서 실제 사용 모델을 식별할 수 있는 이름이다. */
    String modelName();

    /** 후보 순서를 그대로 따를 필요 없이 chunkId와 0~1 관련성 점수를 반환한다. */
    List<ChunkRerankScore> rerank(String query, List<ChunkRerankCandidate> candidates);
}
