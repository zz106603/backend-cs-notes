package com.csnotes.rag.evaluation;

import com.csnotes.rag.search.RagSearchHit;
import com.csnotes.rag.search.RagSearchMode;
import com.csnotes.rag.search.RagSearchRequest;
import com.csnotes.rag.search.RagSearchResponse;
import com.csnotes.rag.search.RagSearchService;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RagEvaluationService {
    private static final List<RagSearchMode> EVALUATION_MODES = List.of(
            RagSearchMode.DENSE, RagSearchMode.SPARSE, RagSearchMode.HYBRID);

    private final RagEvaluationRepository repository;
    private final RagSearchService searchService;
    private final int resultLimit;
    private final Clock clock;

    RagEvaluationService(
            RagEvaluationRepository repository,
            RagSearchService searchService,
            int resultLimit,
            Clock clock
    ) {
        this.repository = repository;
        this.searchService = searchService;
        this.resultLimit = resultLimit;
        this.clock = clock;
    }

    public List<RagEvaluationCase> findAll() {
        return repository.findAll();
    }

    public RagEvaluationCase create(CreateRagEvaluationCaseRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new RagEvaluationValidationException("평가 질문을 입력해 주세요.");
        }
        if (request.query().strip().length() > 500) {
            throw new RagEvaluationValidationException("평가 질문은 500자를 초과할 수 없습니다.");
        }
        Set<String> expectedPaths = new LinkedHashSet<>();
        if (request.expectedDocumentPaths() != null) {
            request.expectedDocumentPaths().stream()
                    .filter(path -> path != null && !path.isBlank())
                    .map(String::strip)
                    .forEach(expectedPaths::add);
        }
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                UUID.randomUUID(), request.query().strip(), List.copyOf(expectedPaths), clock.instant());
        repository.save(evaluationCase);
        return evaluationCase;
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    /** 동일 질문을 세 검색 방식으로 실행해 기대 문서가 실제로 몇 위에 노출되는지 비교한다. */
    public RagEvaluationRunResponse run(UUID id) {
        RagEvaluationCase evaluationCase = repository.findById(id)
                .orElseThrow(() -> new RagEvaluationValidationException("평가 케이스를 찾을 수 없습니다."));
        List<RagEvaluationModeResult> modes = EVALUATION_MODES.stream()
                .map(mode -> evaluateMode(evaluationCase, mode))
                .toList();
        return new RagEvaluationRunResponse(evaluationCase, resultLimit, modes);
    }

    private RagEvaluationModeResult evaluateMode(RagEvaluationCase evaluationCase, RagSearchMode mode) {
        boolean negativeCase = evaluationCase.expectedDocumentPaths().isEmpty();
        // 긍정 평가는 순위 자체를 비교하고, 부정 평가는 운영 기본 임계값으로 불필요한 결과가 노출되는지 확인한다.
        RagSearchResponse response = searchService.search(
                new RagSearchRequest(evaluationCase.query(), resultLimit, negativeCase ? null : 0.0, mode));
        Set<String> expected = Set.copyOf(evaluationCase.expectedDocumentPaths());
        List<String> retrievedRelevantPaths = response.results().stream()
                .map(RagSearchHit::documentPath)
                .filter(expected::contains)
                .distinct()
                .toList();
        Integer firstRelevantRank = null;
        for (int index = 0; index < response.results().size(); index++) {
            if (expected.contains(response.results().get(index).documentPath())) {
                firstRelevantRank = index + 1;
                break;
            }
        }
        // 부정 평가에서는 UI가 결과 유무를 직접 표시하므로 0으로 두어 0/0에 의한 NaN 직렬화를 막는다.
        double recall = negativeCase ? 0 : (double) retrievedRelevantPaths.size() / expected.size();
        double reciprocalRank = firstRelevantRank == null ? 0 : 1.0 / firstRelevantRank;
        return new RagEvaluationModeResult(mode, recall, firstRelevantRank, reciprocalRank, response.results());
    }
}
