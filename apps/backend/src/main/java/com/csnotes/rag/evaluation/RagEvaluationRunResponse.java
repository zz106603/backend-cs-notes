package com.csnotes.rag.evaluation;

import java.util.List;

public record RagEvaluationRunResponse(
        RagEvaluationCase evaluationCase,
        int limit,
        List<RagEvaluationModeResult> modes
) {
}
