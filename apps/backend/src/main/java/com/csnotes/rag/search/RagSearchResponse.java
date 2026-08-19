package com.csnotes.rag.search;

import java.util.List;

public record RagSearchResponse(
        String query,
        String embeddingModel,
        int limit,
        double minimumScore,
        boolean cachedQueryEmbedding,
        List<RagSearchHit> results
) {
}
