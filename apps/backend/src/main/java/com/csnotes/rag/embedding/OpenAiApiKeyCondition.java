package com.csnotes.rag.embedding;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** OPENAI_API_KEY가 실제로 설정된 실행 환경에서만 OpenAI 연동을 활성화한다. */
final class OpenAiApiKeyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("spring.ai.openai.api-key"));
    }
}
