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
        double score
) {
    static RagSearchHit from(ChunkSearchResult result) {
        var chunk = result.chunk();
        return new RagSearchHit(chunk.id(), chunk.documentId(), chunk.documentTitle(), chunk.documentPath(),
                chunk.tags(), chunk.sectionPath(), chunk.content(), result.score());
    }
}
