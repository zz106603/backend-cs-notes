package com.csnotes.rag.reranking.cohere;

import com.csnotes.rag.reranking.ChunkRerankCandidate;
import com.csnotes.rag.reranking.ChunkRerankScore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("cohere-live")
@EnabledIfEnvironmentVariable(named = "COHERE_API_KEY", matches = ".*\\S.*")
class CohereRerankerLiveTest {
    @Test
    void 실제_Cohere가_한국어_질문과_관련된_문서를_상위로_재정렬한다() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.cohere.com")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + System.getenv("COHERE_API_KEY"))
                .defaultHeader("X-Client-Name", "backend-cs-notes-live-test")
                .build();
        CohereChunkReranker reranker = new CohereChunkReranker(
                restClient, new ObjectMapper(), "rerank-v4.0-fast", 1024,
                Duration.ZERO, 10, 1);
        List<ChunkRerankCandidate> candidates = List.of(
                new ChunkRerankCandidate("unrelated", "문자열", "프로그래밍/문자열.md",
                        List.of("문자열 변환"), "문자열을 숫자로 변환하는 방법을 설명한다."),
                new ChunkRerankCandidate("expected", "트랜잭션 격리 수준", "데이터베이스/격리.md",
                        List.of("Dirty Read"), "커밋되지 않은 데이터를 다른 트랜잭션이 읽는 현상을 Dirty Read라고 한다.")
        );

        List<ChunkRerankScore> scores = reranker.rerank(
                "커밋되지 않은 값을 다른 작업이 읽는 문제는 무엇이야?", candidates);

        assertThat(scores).isNotEmpty();
        assertThat(scores.getFirst().chunkId()).isEqualTo("expected");
        assertThat(scores.getFirst().relevanceScore()).isBetween(0.0, 1.0);
    }
}
