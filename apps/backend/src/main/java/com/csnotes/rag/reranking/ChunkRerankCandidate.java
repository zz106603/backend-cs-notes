package com.csnotes.rag.reranking;

import java.util.List;

/** 특정 Reranker SDK에 검색 도메인 객체가 직접 의존하지 않도록 만든 입력 모델이다. */
public record ChunkRerankCandidate(
        String chunkId,
        String documentTitle,
        String documentPath,
        List<String> sectionPath,
        String content
) {
    public ChunkRerankCandidate {
        sectionPath = List.copyOf(sectionPath);
    }
}
