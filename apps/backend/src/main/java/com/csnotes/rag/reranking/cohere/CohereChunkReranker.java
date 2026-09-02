package com.csnotes.rag.reranking.cohere;

import com.csnotes.rag.reranking.ChunkRerankCandidate;
import com.csnotes.rag.reranking.ChunkRerankScore;
import com.csnotes.rag.reranking.ChunkReranker;
import com.csnotes.rag.reranking.ChunkRerankerUnavailableException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cohere Rerank v2 API를 ChunkReranker 포트로 변환하는 HTTP 어댑터다. */
public final class CohereChunkReranker implements ChunkReranker {
    private static final Logger log = LoggerFactory.getLogger(CohereChunkReranker.class);
    private static final String RERANK_PATH = "/v2/rerank";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokensPerDocument;
    private final long cacheTtlNanos;
    private final int cacheMaxEntries;
    private final int maxRequestsPerMinute;
    private final Map<String, CachedScores> cache = new LinkedHashMap<>(16, 0.75f, true);
    private final ArrayDeque<Long> requestTimes = new ArrayDeque<>();

    public CohereChunkReranker(
            RestClient restClient,
            ObjectMapper objectMapper,
            String model,
            int maxTokensPerDocument,
            Duration cacheTtl,
            int cacheMaxEntries,
            int maxRequestsPerMinute
    ) {
        if (model == null || model.isBlank()) throw new IllegalArgumentException("Cohere model must not be blank");
        if (maxTokensPerDocument < 1 || cacheMaxEntries < 1 || maxRequestsPerMinute < 1) {
            throw new IllegalArgumentException("Cohere Reranker limits must be positive");
        }
        if (cacheTtl.isNegative()) throw new IllegalArgumentException("Cohere cache TTL must not be negative");
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.maxTokensPerDocument = maxTokensPerDocument;
        this.cacheTtlNanos = cacheTtl.toNanos();
        this.cacheMaxEntries = cacheMaxEntries;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    public String modelName() {
        return model;
    }

    /** 동일 질문과 동일 Chunk ID 조합은 캐시하고, 실제 호출 직전에 체험판 분당 한도를 방어한다. */
    @Override
    public List<ChunkRerankScore> rerank(String query, List<ChunkRerankCandidate> candidates) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Rerank query must not be blank");
        if (candidates == null || candidates.isEmpty()) return List.of();
        String cacheKey = cacheKey(query, candidates);
        List<ChunkRerankScore> cached = cached(cacheKey);
        if (cached != null) {
            log.debug("Cohere Reranker 캐시 사용: model={}, candidates={}", model, candidates.size());
            return cached;
        }
        acquireRequestPermit();

        List<String> documents = candidates.stream().map(this::toYamlDocument).toList();
        CohereRerankRequest request = new CohereRerankRequest(model, query, documents, maxTokensPerDocument);
        long startedAt = System.nanoTime();
        try {
            CohereRerankResponse response = restClient.post()
                    .uri(RERANK_PATH)
                    .body(request)
                    .retrieve()
                    .body(CohereRerankResponse.class);
            List<ChunkRerankScore> scores = mapScores(response, candidates);
            cache(cacheKey, scores);
            log.info("Cohere Reranker 완료: model={}, candidates={}, elapsedMs={}",
                    model, candidates.size(), (System.nanoTime() - startedAt) / 1_000_000);
            return scores;
        } catch (RestClientResponseException exception) {
            throw new ChunkRerankerUnavailableException("HTTP_" + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new ChunkRerankerUnavailableException("NETWORK_OR_TIMEOUT", exception);
        }
    }

    private List<ChunkRerankScore> mapScores(
            CohereRerankResponse response,
            List<ChunkRerankCandidate> candidates
    ) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new ChunkRerankerUnavailableException("INVALID_RESPONSE");
        }
        List<ChunkRerankScore> scores = new ArrayList<>(response.results().size());
        for (CohereRerankResult result : response.results()) {
            if (result == null || result.index() < 0 || result.index() >= candidates.size()
                    || !Double.isFinite(result.relevanceScore())) {
                throw new ChunkRerankerUnavailableException("INVALID_RESPONSE");
            }
            scores.add(new ChunkRerankScore(
                    candidates.get(result.index()).chunkId(), result.relevanceScore()));
        }
        return List.copyOf(scores);
    }

    /** 제목·경로·섹션도 관련성 판단에 사용하되 JSON 인코딩 문자열을 사용해 YAML 특수문자를 안전하게 처리한다. */
    private String toYamlDocument(ChunkRerankCandidate candidate) {
        return "title: %s%npath: %s%nsection: %s%ncontent: %s".formatted(
                quote(candidate.documentTitle()), quote(candidate.documentPath()),
                quote(String.join(" > ", candidate.sectionPath())), quote(candidate.content()));
    }

    private String quote(String value) {
        try {
            // JSON 문자열은 YAML에서도 유효한 따옴표 문자열이므로 별도 YAML 라이브러리가 필요 없다.
            return objectMapper.writeValueAsString(Objects.requireNonNullElse(value, ""));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cohere document metadata cannot be serialized", exception);
        }
    }

    private String cacheKey(String query, List<ChunkRerankCandidate> candidates) {
        return query + "\n" + candidates.stream()
                .map(ChunkRerankCandidate::chunkId)
                .collect(Collectors.joining("\n"));
    }

    private synchronized List<ChunkRerankScore> cached(String key) {
        CachedScores cached = cache.get(key);
        if (cached == null) return null;
        if (System.nanoTime() - cached.createdAtNanos() > cacheTtlNanos) {
            cache.remove(key);
            return null;
        }
        return cached.scores();
    }

    private synchronized void cache(String key, List<ChunkRerankScore> scores) {
        cache.put(key, new CachedScores(List.copyOf(scores), System.nanoTime()));
        while (cache.size() > cacheMaxEntries) {
            cache.remove(cache.keySet().iterator().next());
        }
    }

    /** Trial 제한 10회/분보다 낮은 기본 8회/분을 로컬에서 보장하며 초과 시 API를 호출하지 않는다. */
    private synchronized void acquireRequestPermit() {
        long now = System.nanoTime();
        long oneMinuteAgo = now - Duration.ofMinutes(1).toNanos();
        while (!requestTimes.isEmpty() && requestTimes.peekFirst() <= oneMinuteAgo) {
            requestTimes.removeFirst();
        }
        if (requestTimes.size() >= maxRequestsPerMinute) {
            throw new ChunkRerankerUnavailableException("LOCAL_RATE_LIMIT");
        }
        requestTimes.addLast(now);
    }

    private record CachedScores(List<ChunkRerankScore> scores, long createdAtNanos) {
    }

    record CohereRerankRequest(
            String model,
            String query,
            List<String> documents,
            @JsonProperty("max_tokens_per_doc") int maxTokensPerDocument
    ) {
    }

    record CohereRerankResponse(List<CohereRerankResult> results) {
    }

    record CohereRerankResult(int index, @JsonProperty("relevance_score") double relevanceScore) {
    }
}
