package com.csnotes.rag.answer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RagAnswerResponse(
        UUID requestId,
        String question,
        String answer,
        String answerModel,
        boolean generated,
        boolean cached,
        int contextCharacters,
        RagAnswerUsage usage,
        BigDecimal estimatedCostUsd,
        List<RagAnswerSource> sources
) {
}
