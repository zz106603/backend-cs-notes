package com.csnotes.rag.answer;

import com.csnotes.rag.search.RagSearchHit;

import java.util.List;

public record RagAnswerSource(
        int number,
        String chunkId,
        String documentId,
        String documentTitle,
        String documentPath,
        List<String> sectionPath,
        double score
) {
    static RagAnswerSource from(int number, RagSearchHit hit) {
        // Reranker가 적용된 경우 출처 화면에도 최종 관련성 점수를 표시하고, 미적용·fallback이면 기존 검색 점수를 사용한다.
        double effectiveScore = hit.rerankScore() == null ? hit.score() : hit.rerankScore();
        return new RagAnswerSource(number, hit.chunkId(), hit.documentId(), hit.documentTitle(),
                hit.documentPath(), hit.sectionPath(), effectiveScore);
    }
}
