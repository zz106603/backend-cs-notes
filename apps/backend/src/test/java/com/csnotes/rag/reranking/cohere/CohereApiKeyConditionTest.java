package com.csnotes.rag.reranking.cohere;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CohereApiKeyConditionTest {
    private final CohereApiKeyCondition condition = new CohereApiKeyCondition();
    private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @Test
    void API_키가_없거나_공백이면_Cohere_어댑터를_생성하지_않는다() {
        assertThat(condition.matches(contextWith(new MockEnvironment()), metadata)).isFalse();
        assertThat(condition.matches(contextWith(new MockEnvironment()
                .withProperty("rag.reranking.cohere.api-key", "   ")), metadata)).isFalse();
    }

    @Test
    void API_키가_있으면_Cohere_어댑터를_생성한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("rag.reranking.cohere.api-key", "test-key");

        assertThat(condition.matches(contextWith(environment), metadata)).isTrue();
    }

    private ConditionContext contextWith(MockEnvironment environment) {
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return context;
    }
}
