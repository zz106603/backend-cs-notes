package com.csnotes.rag.answer;

import com.csnotes.rag.search.RagSearchHit;
import com.csnotes.rag.search.RagSearchRequest;
import com.csnotes.rag.search.RagSearchResponse;
import com.csnotes.rag.search.RagSearchService;
import com.csnotes.rag.search.RagSearchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** retrieval 결과를 제한된 컨텍스트로 조립해 근거 기반 답변을 생성한다. */
public final class RagAnswerService {
    private static final Logger log = LoggerFactory.getLogger(RagAnswerService.class);
    private static final String NO_EVIDENCE_MESSAGE = "관련 학습 문서를 찾지 못해 답변을 생성하지 않았습니다.";

    private final RagSearchService searchService;
    private final RagAnswerGenerator answerGenerator;
    private final int defaultSourceLimit;
    private final int maxSourceLimit;
    private final int maxContextCharacters;
    private final double defaultMinimumScore;
    private final long cacheTtlNanos;
    private final int cacheMaxEntries;
    private final RagAnswerUsageStore usageStore;
    private final RagAnswerCostPolicy costPolicy;
    private final Clock clock;
    private final Map<String, CachedAnswer> answerCache = new LinkedHashMap<>(16, 0.75f, true);
    private final Set<String> inProgressAnswers = ConcurrentHashMap.newKeySet();
    private final Object budgetLock = new Object();
    private BigDecimal reservedCostUsd = BigDecimal.ZERO;

    public RagAnswerService(
            RagSearchService searchService,
            RagAnswerGenerator answerGenerator,
            int defaultSourceLimit,
            int maxSourceLimit,
            int maxContextCharacters,
            double defaultMinimumScore,
            Duration cacheTtl,
            int cacheMaxEntries,
            RagAnswerUsageStore usageStore,
            RagAnswerCostPolicy costPolicy,
            Clock clock
    ) {
        if (defaultSourceLimit < 1 || defaultSourceLimit > maxSourceLimit) {
            throw new IllegalArgumentException("Default source limit must be within the maximum source limit");
        }
        if (maxContextCharacters < 1 || cacheMaxEntries < 1 || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("RAG answer limits must be positive");
        }
        this.searchService = searchService;
        this.answerGenerator = answerGenerator;
        this.defaultSourceLimit = defaultSourceLimit;
        this.maxSourceLimit = maxSourceLimit;
        this.maxContextCharacters = maxContextCharacters;
        this.defaultMinimumScore = defaultMinimumScore;
        this.cacheTtlNanos = cacheTtl.toNanos();
        this.cacheMaxEntries = cacheMaxEntries;
        this.usageStore = usageStore;
        this.costPolicy = costPolicy;
        this.clock = clock;
    }

    public RagAnswerResponse answer(RagAnswerRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new RagAnswerValidationException("질문을 입력해 주세요.");
        }
        int sourceLimit = request.sourceLimit() == null ? defaultSourceLimit : request.sourceLimit();
        if (sourceLimit < 1 || sourceLimit > maxSourceLimit) {
            throw new RagAnswerValidationException("참고 자료 수는 1~" + maxSourceLimit + " 사이여야 합니다.");
        }
        double minimumScore = request.minimumScore() == null ? defaultMinimumScore : request.minimumScore();
        if (!Double.isFinite(minimumScore) || minimumScore < 0 || minimumScore > 1) {
            throw new RagAnswerValidationException("최소 유사도는 0~1 사이여야 합니다.");
        }

        String question = request.question().strip();
        if (question.length() > 500) {
            throw new RagAnswerValidationException("질문은 500자 이내로 입력해 주세요.");
        }
        UUID requestId = UUID.randomUUID();
        long requestStartedAt = System.nanoTime();
        RagSearchResponse search = searchService.search(
                new RagSearchRequest(question, sourceLimit, minimumScore, RagSearchMode.DENSE));
        if (search.results().isEmpty()) {
            RagAnswerResponse response = new RagAnswerResponse(requestId, question, NO_EVIDENCE_MESSAGE,
                    answerGenerator.modelName(), false, false, 0, RagAnswerUsage.unknown(), BigDecimal.ZERO, List.of());
            saveUsage(requestId, question, "NO_EVIDENCE", RagAnswerUsage.unknown(), BigDecimal.ZERO,
                    0, 0, requestStartedAt, null);
            log.info("RAG 답변 생략: 검색 근거 없음, model={}", answerGenerator.modelName());
            return response;
        }

        ContextBundle context = buildContext(search.results());
        String cacheKey = question + "\n" + context.hits().stream()
                .map(RagSearchHit::chunkId)
                .collect(Collectors.joining("\n"));
        RagAnswerResponse cached = cached(cacheKey);
        if (cached != null) {
            saveUsage(requestId, question, "CACHED", cached.usage(), BigDecimal.ZERO,
                    cached.sources().size(), cached.contextCharacters(), requestStartedAt, null);
            return withCached(cached, requestId);
        }

        BigDecimal maximumCost = costPolicy.estimateMaximumCost(question.length(), context.text().length());
        if (!inProgressAnswers.add(cacheKey)) throw new RagAnswerInProgressException();
        try {
            reserveBudget(maximumCost);
        } catch (RuntimeException exception) {
            inProgressAnswers.remove(cacheKey);
            throw exception;
        }

        long startedAt = System.nanoTime();
        try {
            GeneratedAnswer generated = answerGenerator.generate(question, context.text());
            List<RagAnswerSource> sources = IntStream.range(0, context.hits().size())
                    .mapToObj(index -> RagAnswerSource.from(index + 1, context.hits().get(index)))
                    .toList();
            BigDecimal estimatedCost = costPolicy.estimateActualCost(
                    generated.usage(), question.length(), context.text().length());
            RagAnswerResponse response = new RagAnswerResponse(requestId, question, generated.content(),
                    answerGenerator.modelName(), true, false, context.text().length(), generated.usage(),
                    estimatedCost, sources);
            saveUsage(requestId, question, "GENERATED", generated.usage(), estimatedCost,
                    sources.size(), context.text().length(), requestStartedAt, null);
            cache(cacheKey, response);
            log.info("RAG 답변 완료: requestId={}, model={}, sources={}, contextCharacters={}, totalTokens={}, estimatedCostUsd={}, elapsedMs={}",
                    requestId, answerGenerator.modelName(), sources.size(), context.text().length(),
                    generated.usage().totalTokens(), estimatedCost, (System.nanoTime() - startedAt) / 1_000_000);
            return response;
        } catch (RuntimeException exception) {
            saveUsage(requestId, question, "FAILED", RagAnswerUsage.unknown(), maximumCost,
                    context.hits().size(), context.text().length(), requestStartedAt, exception.getClass().getSimpleName());
            log.error("RAG 답변 실패: requestId={}, model={}, failureType={}",
                    requestId, answerGenerator.modelName(), exception.getClass().getSimpleName(), exception);
            throw exception;
        } finally {
            releaseBudget(maximumCost);
            inProgressAnswers.remove(cacheKey);
        }
    }

    /** 프롬프트 비용 상한을 지키면서 출처 번호와 메타데이터를 본문에 함께 넣는다. */
    private ContextBundle buildContext(List<RagSearchHit> hits) {
        StringBuilder context = new StringBuilder();
        List<RagSearchHit> included = new ArrayList<>();
        for (RagSearchHit hit : hits) {
            String separator = context.isEmpty() ? "" : "\n\n";
            int number = included.size() + 1;
            String header = "[자료 %d]\n문서: %s\n경로: %s\n섹션: %s\n본문:\n".formatted(
                    number, hit.documentTitle(), hit.documentPath(), String.join(" > ", hit.sectionPath())
            );
            int contentCapacity = maxContextCharacters - context.length() - separator.length() - header.length();
            if (contentCapacity < 1) break;
            String content = hit.content().substring(0, Math.min(hit.content().length(), contentCapacity));
            context.append(separator).append(header).append(content);
            included.add(hit);
            if (context.length() >= maxContextCharacters) break;
        }
        return new ContextBundle(context.toString(), List.copyOf(included));
    }

    private synchronized RagAnswerResponse cached(String key) {
        CachedAnswer cached = answerCache.get(key);
        if (cached == null) return null;
        if (System.nanoTime() - cached.createdAtNanos() > cacheTtlNanos) {
            answerCache.remove(key);
            return null;
        }
        return cached.response();
    }

    private synchronized void cache(String key, RagAnswerResponse response) {
        answerCache.put(key, new CachedAnswer(response, System.nanoTime()));
        while (answerCache.size() > cacheMaxEntries) {
            answerCache.remove(answerCache.keySet().iterator().next());
        }
    }

    private RagAnswerResponse withCached(RagAnswerResponse response, UUID requestId) {
        return new RagAnswerResponse(requestId, response.question(), response.answer(), response.answerModel(),
                response.generated(), true, response.contextCharacters(), response.usage(), BigDecimal.ZERO,
                response.sources());
    }

    private BigDecimal spentToday() {
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        return usageStore.totalCostBetween(from, to);
    }

    /** 동시에 들어온 서로 다른 질문도 아직 기록되지 않은 최대 예상 비용까지 예약해 한도를 지킨다. */
    private void reserveBudget(BigDecimal maximumCost) {
        synchronized (budgetLock) {
            BigDecimal spentAndReserved = spentToday().add(reservedCostUsd);
            if (costPolicy.exceedsDailyLimit(spentAndReserved, maximumCost)) {
                log.warn("RAG 답변 차단: 일일 비용 한도, spentAndReservedUsd={}, nextMaximumUsd={}, limitUsd={}",
                        spentAndReserved, maximumCost, costPolicy.dailyLimitUsd());
                throw new RagAnswerLimitExceededException(
                        "오늘의 예상 비용 한도 $%s를 초과할 수 있어 요청을 중단했습니다."
                                .formatted(costPolicy.dailyLimitUsd().stripTrailingZeros().toPlainString()));
            }
            reservedCostUsd = reservedCostUsd.add(maximumCost);
        }
    }

    private void releaseBudget(BigDecimal maximumCost) {
        synchronized (budgetLock) {
            reservedCostUsd = reservedCostUsd.subtract(maximumCost);
        }
    }

    private void saveUsage(UUID requestId, String question, String status, RagAnswerUsage usage,
                           BigDecimal estimatedCost, int sourceCount, int contextCharacters,
                           long startedAtNanos, String failureType) {
        usageStore.save(new RagAnswerUsageRecord(requestId, sha256(question), answerGenerator.modelName(), status,
                usage, estimatedCost, sourceCount, contextCharacters,
                (System.nanoTime() - startedAtNanos) / 1_000_000, failureType, clock.instant()));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record ContextBundle(String text, List<RagSearchHit> hits) {
    }

    private record CachedAnswer(RagAnswerResponse response, long createdAtNanos) {
    }
}
