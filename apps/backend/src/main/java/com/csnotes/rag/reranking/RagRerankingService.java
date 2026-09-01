package com.csnotes.rag.reranking;

import com.csnotes.rag.search.RagSearchHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 모델 호출 구현과 검색 흐름 사이에서 점수 검증, 필터링, 재정렬을 담당한다. */
public final class RagRerankingService {
    private final boolean enabled;
    private final ChunkReranker reranker;
    private final double minimumScore;

    public RagRerankingService(boolean enabled, ChunkReranker reranker, double minimumScore) {
        if (enabled && reranker == null) {
            throw new IllegalArgumentException("Enabled reranking requires a ChunkReranker");
        }
        if (!Double.isFinite(minimumScore) || minimumScore < 0 || minimumScore > 1) {
            throw new IllegalArgumentException("Reranking minimum score must be between 0 and 1");
        }
        this.enabled = enabled;
        this.reranker = reranker;
        this.minimumScore = minimumScore;
    }

    public static RagRerankingService disabled() {
        return new RagRerankingService(false, null, 0);
    }

    /**
     * RRF 후보를 모델 입력으로 변환하고 관련성 점수에 따라 다시 정렬한다.
     * 비활성 상태에서는 모델을 호출하지 않고 기존 RRF 순서를 그대로 사용한다.
     */
    public List<RagSearchHit> rerank(String query, List<RagSearchHit> candidates, int limit) {
        if (!enabled) return candidates.stream().limit(limit).toList();

        // 모델 SDK에 RagSearchHit 전체를 넘기지 않아 검색 도메인과 외부 구현의 결합을 막는다.
        List<ChunkRerankCandidate> inputs = candidates.stream()
                .map(hit -> new ChunkRerankCandidate(hit.chunkId(), hit.documentTitle(), hit.documentPath(),
                        hit.sectionPath(), hit.content()))
                .toList();
        List<ChunkRerankScore> scores = reranker.rerank(query, inputs);
        Map<String, RagSearchHit> hitsById = new HashMap<>();
        Map<String, Integer> originalRanks = new HashMap<>();
        for (int index = 0; index < candidates.size(); index++) {
            RagSearchHit hit = candidates.get(index);
            hitsById.put(hit.chunkId(), hit);
            originalRanks.put(hit.chunkId(), index + 1);
        }

        Set<String> scoredIds = new HashSet<>();
        List<ScoredHit> scoredHits = new ArrayList<>();
        // 모델이 점수를 반환하지 않은 후보와 minimumScore 미만 후보는 최종 검색 근거에서 제외된다.
        for (ChunkRerankScore score : scores) {
            // 외부 모델 응답을 그대로 신뢰하지 않고 요청 후보와 일치하는지 경계에서 검증한다.
            if (!hitsById.containsKey(score.chunkId())) {
                throw new IllegalStateException("Reranker returned an unknown chunk: " + score.chunkId());
            }
            if (!scoredIds.add(score.chunkId())) {
                throw new IllegalStateException("Reranker returned a duplicate chunk: " + score.chunkId());
            }
            if (!Double.isFinite(score.relevanceScore())
                    || score.relevanceScore() < 0 || score.relevanceScore() > 1) {
                throw new IllegalStateException("Reranker score must be between 0 and 1");
            }
            if (score.relevanceScore() >= minimumScore) {
                scoredHits.add(new ScoredHit(hitsById.get(score.chunkId()), score.relevanceScore(),
                        originalRanks.get(score.chunkId())));
            }
        }

        // 관련성 점수가 같으면 RRF 원래 순위를 유지해 결과가 실행마다 바뀌지 않게 한다.
        scoredHits.sort(Comparator.comparingDouble(ScoredHit::score).reversed()
                .thenComparingInt(ScoredHit::originalRank));
        List<RagSearchHit> results = new ArrayList<>();
        for (int index = 0; index < Math.min(limit, scoredHits.size()); index++) {
            ScoredHit scored = scoredHits.get(index);
            // RRF 점수는 score에 보존하고 Reranker 점수와 새 순위는 별도 필드에 기록한다.
            results.add(scored.hit().withRerank(scored.score(), index + 1));
        }
        return List.copyOf(results);
    }

    private record ScoredHit(RagSearchHit hit, double score, int originalRank) {
    }
}
