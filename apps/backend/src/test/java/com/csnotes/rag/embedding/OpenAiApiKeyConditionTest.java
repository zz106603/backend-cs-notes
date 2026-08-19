package com.csnotes.rag.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiApiKeyConditionTest {
    private final OpenAiApiKeyCondition condition = new OpenAiApiKeyCondition();
    private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @Test
    void API_키가_없으면_OpenAI_연동을_비활성화한다() {
        assertThat(condition.matches(contextWith(new MockEnvironment()), metadata)).isFalse();
    }

    @Test
    void API_키가_공백이면_OpenAI_연동을_비활성화한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "   ");

        assertThat(condition.matches(contextWith(environment), metadata)).isFalse();
    }

    @Test
    void API_키가_있으면_OpenAI_연동을_활성화한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "test-key");

        assertThat(condition.matches(contextWith(environment), metadata)).isTrue();
    }

    private ConditionContext contextWith(MockEnvironment environment) {
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return context;
    }
}
