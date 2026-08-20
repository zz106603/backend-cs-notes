package com.csnotes.rag.answer;

import java.math.BigDecimal;
import java.time.Instant;

interface RagAnswerUsageStore {
    BigDecimal totalCostBetween(Instant fromInclusive, Instant toExclusive);

    void save(RagAnswerUsageRecord record);
}

