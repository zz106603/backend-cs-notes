package com.csnotes.rag.reranking;

import com.csnotes.rag.search.RagSearchHit;

import java.util.List;

/** 재정렬 결과와 함께 이번 요청에서 임계값이 실제 적용됐는지를 전달한다. */
public record RagRerankingResult(
        List<RagSearchHit> hits,
        boolean applied,
        Double minimumScore
) {
    public static RagRerankingResult applied(List<RagSearchHit> hits, double minimumScore) {
        return new RagRerankingResult(List.copyOf(hits), true, minimumScore);
    }

    public static RagRerankingResult fallback(List<RagSearchHit> hits) {
        return new RagRerankingResult(List.copyOf(hits), false, null);
    }
}
