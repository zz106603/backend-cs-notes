package com.csnotes.rag.search;

import com.csnotes.rag.persistence.ChunkSearchResult;

import java.util.List;

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
                List.of(mode));
    }
}
