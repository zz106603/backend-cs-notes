package com.csnotes.rag.answer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record RagAnswerUsageRecord(
        UUID requestId,
        String questionHash,
        String model,
        String status,
        RagAnswerUsage usage,
        BigDecimal estimatedCostUsd,
        int sourceCount,
        int contextCharacters,
        long elapsedMs,
        String failureType,
        Instant createdAt
) {
}

