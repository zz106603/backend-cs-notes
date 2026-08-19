package com.csnotes.rag.search;

import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkVectorStore;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 질의를 임베딩하고 pgvector cosine 검색 결과를 출처 메타데이터와 함께 반환한다. */
public final class RagSearchService {
    private final EmbeddingProvider embeddingProvider;
    private final ChunkVectorStore vectorStore;
    private final int defaultLimit;
    private final int maxLimit;
    private final int maxQueryCharacters;
    private final double defaultMinimumScore;
    private final long cacheTtlNanos;
    private final int cacheMaxEntries;
    private final Map<String, CachedEmbedding> queryCache = new LinkedHashMap<>(16, 0.75f, true);

    public RagSearchService(
            EmbeddingProvider embeddingProvider,
            ChunkVectorStore vectorStore,
            int defaultLimit,
            int maxLimit,
            int maxQueryCharacters,
            double defaultMinimumScore,
            Duration cacheTtl,
            int cacheMaxEntries
    ) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.maxQueryCharacters = maxQueryCharacters;
        this.defaultMinimumScore = defaultMinimumScore;
        this.cacheTtlNanos = cacheTtl.toNanos();
        this.cacheMaxEntries = cacheMaxEntries;
    }

    public RagSearchResponse search(RagSearchRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new RagSearchValidationException("검색어를 입력해 주세요.");
        }
        String query = request.query().strip();
        if (query.length() > maxQueryCharacters) {
            throw new RagSearchValidationException("검색어는 " + maxQueryCharacters + "자를 초과할 수 없습니다.");
        }
        int limit = request.limit() == null ? defaultLimit : request.limit();
        if (limit < 1 || limit > maxLimit) {
            throw new RagSearchValidationException("검색 결과 수는 1~" + maxLimit + " 사이여야 합니다.");
        }
        double minimumScore = request.minimumScore() == null ? defaultMinimumScore : request.minimumScore();
        if (!Double.isFinite(minimumScore) || minimumScore < 0 || minimumScore > 1) {
            throw new RagSearchValidationException("최소 유사도는 0~1 사이여야 합니다.");
        }

        CachedLookup lookup = queryEmbedding(query);
        List<RagSearchHit> results = vectorStore.search(lookup.embedding(), limit, minimumScore).stream()
                .map(RagSearchHit::from)
                .toList();
        return new RagSearchResponse(query, embeddingProvider.modelName(), limit, minimumScore,
                lookup.cached(), results);
    }

    /** 같은 프로세스에서 반복한 동일 질의는 짧게 캐시해 불필요한 유료 API 호출을 막는다. */
    private synchronized CachedLookup queryEmbedding(String query) {
        long now = System.nanoTime();
        CachedEmbedding cached = queryCache.get(query);
        if (cached != null && now - cached.createdAtNanos() <= cacheTtlNanos) {
            return new CachedLookup(cached.embedding(), true);
        }
        queryCache.remove(query);
        EmbeddingVector embedding = embeddingProvider.embedQuery(query);
        queryCache.put(query, new CachedEmbedding(embedding, now));
        while (queryCache.size() > cacheMaxEntries) {
            queryCache.remove(queryCache.keySet().iterator().next());
        }
        return new CachedLookup(embedding, false);
    }

    private record CachedEmbedding(EmbeddingVector embedding, long createdAtNanos) {
    }

    private record CachedLookup(EmbeddingVector embedding, boolean cached) {
    }
}
