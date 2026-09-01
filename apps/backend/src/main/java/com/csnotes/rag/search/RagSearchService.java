package com.csnotes.rag.search;

import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkSearchResult;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.reranking.RagRerankingService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private final double defaultDenseMinimumScore;
    private final double defaultSparseMinimumScore;
    private final int hybridCandidateLimit;
    private final int hybridRrfK;
    private final RagRerankingService rerankingService;
    private final long cacheTtlNanos;
    private final int cacheMaxEntries;
    private final Map<String, CachedEmbedding> queryCache = new LinkedHashMap<>(16, 0.75f, true);

    public RagSearchService(
            EmbeddingProvider embeddingProvider,
            ChunkVectorStore vectorStore,
            int defaultLimit,
            int maxLimit,
            int maxQueryCharacters,
            double defaultDenseMinimumScore,
            double defaultSparseMinimumScore,
            int hybridCandidateLimit,
            int hybridRrfK,
            RagRerankingService rerankingService,
            Duration cacheTtl,
            int cacheMaxEntries
    ) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.maxQueryCharacters = maxQueryCharacters;
        this.defaultDenseMinimumScore = defaultDenseMinimumScore;
        this.defaultSparseMinimumScore = defaultSparseMinimumScore;
        this.hybridCandidateLimit = hybridCandidateLimit;
        this.hybridRrfK = hybridRrfK;
        this.rerankingService = rerankingService;
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
        RagSearchMode mode = request.mode() == null ? RagSearchMode.DENSE : request.mode();
        double defaultMinimumScore = mode == RagSearchMode.SPARSE
                ? defaultSparseMinimumScore : defaultDenseMinimumScore;
        double minimumScore = request.minimumScore() == null ? defaultMinimumScore : request.minimumScore();
        if (!Double.isFinite(minimumScore) || minimumScore < 0 || minimumScore > 1) {
            throw new RagSearchValidationException("최소 유사도는 0~1 사이여야 합니다.");
        }

        if (mode == RagSearchMode.SPARSE) {
            List<RagSearchHit> results = toHits(
                    vectorStore.searchSparse(query, limit, minimumScore), RagSearchMode.SPARSE);
            return new RagSearchResponse(query, null, mode, limit, minimumScore, false, results);
        }

        if (embeddingProvider == null) {
            throw new RagSearchValidationException("의미 검색에는 OpenAI API 키가 필요합니다.");
        }
        CachedLookup lookup = queryEmbedding(query);
        if (mode == RagSearchMode.HYBRID) {
            // 최종 결과보다 넓은 후보군을 각각 조회해야 두 검색 결과를 합친 뒤에도 충분한 결과가 남는다.
            int candidateLimit = Math.max(limit, hybridCandidateLimit);
            List<ChunkSearchResult> denseResults = vectorStore.search(
                    lookup.embedding(), candidateLimit, minimumScore);
            // Sparse 점수는 Dense 유사도와 척도가 다르므로 Dense 최소 점수를 적용하지 않는다.
            List<ChunkSearchResult> sparseResults = vectorStore.searchSparse(
                    query, candidateLimit, defaultSparseMinimumScore);
            // RRF는 후보를 넓게 정리하고, Reranker가 있으면 질문과 본문을 직접 비교한 뒤 최종 limit만 남긴다.
            List<RagSearchHit> fusedCandidates = reciprocalRankFusion(
                    denseResults, sparseResults, candidateLimit);
            List<RagSearchHit> results = rerankingService.rerank(query, fusedCandidates, limit);
            return new RagSearchResponse(query, embeddingProvider.modelName(), mode, limit, minimumScore,
                    lookup.cached(), results);
        }

        List<RagSearchHit> results = toHits(
                vectorStore.search(lookup.embedding(), limit, minimumScore), RagSearchMode.DENSE);
        return new RagSearchResponse(query, embeddingProvider.modelName(), mode, limit, minimumScore,
                lookup.cached(), results);
    }

    private List<RagSearchHit> toHits(List<ChunkSearchResult> results, RagSearchMode mode) {
        List<RagSearchHit> hits = new ArrayList<>(results.size());
        for (int index = 0; index < results.size(); index++) {
            hits.add(RagSearchHit.from(results.get(index), mode, index + 1));
        }
        return hits;
    }

    /** Dense와 Sparse의 점수 척도 대신 순위를 이용해 결과를 안정적으로 합친다. */
    private List<RagSearchHit> reciprocalRankFusion(
            List<ChunkSearchResult> denseResults,
            List<ChunkSearchResult> sparseResults,
            int limit
    ) {
        Map<String, HybridCandidate> candidates = new HashMap<>();
        addCandidates(candidates, denseResults, RagSearchMode.DENSE);
        addCandidates(candidates, sparseResults, RagSearchMode.SPARSE);
        double maximumRrfScore = 2.0 / (hybridRrfK + 1.0);

        return candidates.values().stream()
                .map(candidate -> candidate.toHit(hybridRrfK, maximumRrfScore))
                .sorted(Comparator.comparingDouble(RagSearchHit::score).reversed()
                        .thenComparing(RagSearchHit::chunkId))
                .limit(limit)
                .toList();
    }

    private void addCandidates(
            Map<String, HybridCandidate> candidates,
            List<ChunkSearchResult> results,
            RagSearchMode mode
    ) {
        for (int index = 0; index < results.size(); index++) {
            ChunkSearchResult result = results.get(index);
            // 같은 Chunk가 양쪽 결과에 등장하면 chunk ID를 기준으로 하나의 후보에 합친다.
            candidates.computeIfAbsent(result.chunk().id(), ignored -> new HybridCandidate(result))
                    .add(mode, index + 1, result.score());
        }
    }

    private static final class HybridCandidate {
        private final ChunkSearchResult result;
        private Double denseScore;
        private Double sparseScore;
        private Integer denseRank;
        private Integer sparseRank;

        private HybridCandidate(ChunkSearchResult result) {
            this.result = result;
        }

        /** 검색 방식별 원본 점수와 1부터 시작하는 순위를 보존해 결과 근거로 제공한다. */
        private void add(RagSearchMode mode, int rank, double score) {
            if (mode == RagSearchMode.DENSE) {
                denseScore = score;
                denseRank = rank;
            } else {
                sparseScore = score;
                sparseRank = rank;
            }
        }

        /** RRF 공식의 각 순위 기여도를 더하고, 화면 표시용으로 0~1 범위에 정규화한다. */
        private RagSearchHit toHit(int rrfK, double maximumRrfScore) {
            // rrfK는 상위 순위 간 점수 차이가 지나치게 커지는 것을 완화하는 상수다.
            double rawScore = (denseRank == null ? 0 : 1.0 / (rrfK + denseRank))
                    + (sparseRank == null ? 0 : 1.0 / (rrfK + sparseRank));
            List<RagSearchMode> matchedBy = new ArrayList<>(2);
            if (denseRank != null) matchedBy.add(RagSearchMode.DENSE);
            if (sparseRank != null) matchedBy.add(RagSearchMode.SPARSE);
            var chunk = result.chunk();
            // 정규화는 표시값만 바꾸며 모든 후보에 같은 값을 나누므로 최종 순서는 변하지 않는다.
            return new RagSearchHit(chunk.id(), chunk.documentId(), chunk.documentTitle(),
                    chunk.documentPath(), chunk.tags(), chunk.sectionPath(), chunk.content(),
                    rawScore / maximumRrfScore, denseScore, sparseScore, denseRank, sparseRank,
                    null, null, matchedBy);
        }
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
