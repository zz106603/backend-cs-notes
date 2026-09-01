package com.csnotes.rag.search;

import com.csnotes.rag.persistence.ChunkSearchResult;

import java.util.List;

/** RRF 정보와 선택적인 Reranker 점수를 함께 보존해 재정렬 전후를 비교할 수 있는 검색 결과다. */
public record RagSearchHit(
        String chunkId,
        String documentId,
        String documentTitle,
        String documentPath,
        List<String> tags,
        List<String> sectionPath,
        String content,
        double score,
        Double denseScore,
        Double sparseScore,
        Integer denseRank,
        Integer sparseRank,
        Double rerankScore,
        Integer rerankRank,
        List<RagSearchMode> matchedBy
) {
    static RagSearchHit from(ChunkSearchResult result, RagSearchMode mode, int rank) {
        var chunk = result.chunk();
        return new RagSearchHit(chunk.id(), chunk.documentId(), chunk.documentTitle(), chunk.documentPath(),
                chunk.tags(), chunk.sectionPath(), chunk.content(), result.score(),
                mode == RagSearchMode.DENSE ? result.score() : null,
                mode == RagSearchMode.SPARSE ? result.score() : null,
                mode == RagSearchMode.DENSE ? rank : null,
                mode == RagSearchMode.SPARSE ? rank : null,
                null, null,
                List.of(mode));
    }

    public RagSearchHit withRerank(double relevanceScore, int rank) {
        return new RagSearchHit(chunkId, documentId, documentTitle, documentPath, tags, sectionPath, content,
                score, denseScore, sparseScore, denseRank, sparseRank, relevanceScore, rank, matchedBy);
    }
}
