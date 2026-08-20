package com.csnotes.rag.answer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("rag-answer-live")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*\\S.*")
@SpringBootTest(properties = {
        "rag.persistence.enabled=true",
        "rag.search.enabled=true",
        "rag.answer.enabled=true"
})
class RagAnswerLiveTest {
    @Autowired
    private RagAnswerService answerService;

    @Test
    void 실제_검색_청크를_근거로_OpenAI_답변과_출처를_반환한다() {
        RagAnswerResponse response = answerService.answer(
                new RagAnswerRequest("Spring 트랜잭션 전파는 어떻게 동작하나요?", 4, 0.0)
        );

        assertThat(response.generated())
                .as("먼저 M4.5 실제 색인을 실행해 document_chunk 데이터를 저장해야 합니다.")
                .isTrue();
        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).isNotEmpty();
        assertThat(response.contextCharacters()).isPositive();
    }
}
