package com.csnotes.rag.answer;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 실제 토큰 사용량을 가격 설정에 적용하고 호출 전에는 보수적인 최대 비용을 계산한다. */
final class RagAnswerCostPolicy {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final BigDecimal inputPricePerMillion;
    private final BigDecimal outputPricePerMillion;
    private final BigDecimal dailyLimitUsd;
    private final int maxOutputTokens;

    RagAnswerCostPolicy(BigDecimal inputPricePerMillion, BigDecimal outputPricePerMillion,
                        BigDecimal dailyLimitUsd, int maxOutputTokens) {
        if (inputPricePerMillion.signum() < 0 || outputPricePerMillion.signum() < 0
                || dailyLimitUsd.signum() <= 0 || maxOutputTokens < 1) {
            throw new IllegalArgumentException("RAG answer cost settings must be positive");
        }
        this.inputPricePerMillion = inputPricePerMillion;
        this.outputPricePerMillion = outputPricePerMillion;
        this.dailyLimitUsd = dailyLimitUsd;
        this.maxOutputTokens = maxOutputTokens;
    }

    BigDecimal estimateMaximumCost(int questionCharacters, int contextCharacters) {
        // 한글은 문자당 토큰 비율이 높을 수 있어 입력 문자 수를 토큰 수로 간주해 상한을 잡는다.
        int estimatedInputTokens = questionCharacters + contextCharacters + 500;
        return cost(estimatedInputTokens, maxOutputTokens);
    }

    BigDecimal estimateActualCost(RagAnswerUsage usage, int questionCharacters, int contextCharacters) {
        if (usage.promptTokens() == null || usage.completionTokens() == null) {
            return estimateMaximumCost(questionCharacters, contextCharacters);
        }
        return cost(usage.promptTokens(), usage.completionTokens());
    }

    boolean exceedsDailyLimit(BigDecimal spentToday, BigDecimal nextMaximumCost) {
        return spentToday.add(nextMaximumCost).compareTo(dailyLimitUsd) > 0;
    }

    BigDecimal dailyLimitUsd() {
        return dailyLimitUsd;
    }

    private BigDecimal cost(int inputTokens, int outputTokens) {
        return inputPricePerMillion.multiply(BigDecimal.valueOf(inputTokens))
                .add(outputPricePerMillion.multiply(BigDecimal.valueOf(outputTokens)))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
    }
}

