package com.csnotes.rag.search;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("rag-search-live")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*\\S.*")
@SpringBootTest(properties = {
        "rag.persistence.enabled=true",
        "rag.search.enabled=true"
})
class RagSearchLiveTest {
    @Autowired
    private RagSearchService searchService;

    @Test
    void 실제_pgvector에서_트랜잭션_관련_청크를_유사도순으로_반환한다() {
        RagSearchResponse response = searchService.search(
                new RagSearchRequest("Spring 트랜잭션 전파는 어떻게 동작하나요?", 5, 0.0)
        );

        assertThat(response.results())
                .as("먼저 M4.5 실제 색인을 실행해 document_chunk 데이터를 저장해야 합니다.")
                .isNotEmpty();
        assertThat(response.results()).extracting(RagSearchHit::score).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(response.results()).allSatisfy(result -> {
            assertThat(result.score()).isBetween(0.0, 1.0);
            assertThat(result.documentPath()).endsWith(".md");
            assertThat(result.content()).isNotBlank();
        });
    }
}
