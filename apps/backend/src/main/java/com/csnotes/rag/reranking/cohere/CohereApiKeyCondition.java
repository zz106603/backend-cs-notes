package com.csnotes.rag.reranking.cohere;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** COHERE_API_KEY가 실제 값으로 주입된 경우에만 외부 API 어댑터를 생성한다. */
public final class CohereApiKeyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String apiKey = context.getEnvironment().getProperty("rag.reranking.cohere.api-key");
        return apiKey != null && !apiKey.isBlank();
    }
}
