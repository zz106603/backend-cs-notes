package com.csnotes.rag.evaluation;

import com.csnotes.rag.search.RagSearchHit;
import com.csnotes.rag.search.RagSearchMode;

import java.util.List;

public record RagEvaluationModeResult(
        RagSearchMode mode,
        double recallAtLimit,
        Integer firstRelevantRank,
        double reciprocalRank,
        List<RagSearchHit> results,
        boolean rerankingApplied,
        Double rerankingMinimumScore
) {
}
