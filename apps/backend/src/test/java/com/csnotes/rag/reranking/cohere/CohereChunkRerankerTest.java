package com.csnotes.rag.reranking.cohere;

import com.csnotes.rag.reranking.ChunkRerankCandidate;
import com.csnotes.rag.reranking.ChunkRerankScore;
import com.csnotes.rag.reranking.ChunkRerankerUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.test.json.JsonCompareMode;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

class CohereChunkRerankerTest {
    @Test
    void 질문과_YAML_문서를_전송하고_응답_index를_Chunk_ID로_복원한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.cohere.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CohereChunkReranker reranker = reranker(builder.build(), 8);
        server.expect(once(), requestTo("https://api.cohere.test/v2/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "rerank-v4.0-fast",
                          "query": "트랜잭션 질문",
                          "max_tokens_per_doc": 1024
                        }
                        """, JsonCompareMode.LENIENT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("title: \\\"문서 B\\\"")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"index":1,"relevance_score":0.91},
                          {"index":0,"relevance_score":0.32}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<ChunkRerankScore> first = reranker.rerank("트랜잭션 질문", candidates());
        List<ChunkRerankScore> cached = reranker.rerank("트랜잭션 질문", candidates());

        assertThat(first).containsExactly(
                new ChunkRerankScore("chunk-b", 0.91),
                new ChunkRerankScore("chunk-a", 0.32));
        assertThat(cached).isEqualTo(first);
        server.verify();
    }

    @Test
    void Cohere가_429를_반환하면_fallback_가능한_예외로_변환한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.cohere.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CohereChunkReranker reranker = reranker(builder.build(), 8);
        server.expect(requestTo("https://api.cohere.test/v2/rerank"))
                .andRespond(withTooManyRequests());

        assertThatThrownBy(() -> reranker.rerank("질문", candidates()))
                .isInstanceOfSatisfying(ChunkRerankerUnavailableException.class,
                        exception -> assertThat(exception.reason()).isEqualTo("HTTP_429"));
    }

    @Test
    void 응답_index가_후보_범위를_벗어나면_잘못된_외부_응답으로_처리한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.cohere.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CohereChunkReranker reranker = reranker(builder.build(), 8);
        server.expect(requestTo("https://api.cohere.test/v2/rerank"))
                .andRespond(withSuccess("{" + "\"results\":[{\"index\":9,\"relevance_score\":0.8}]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> reranker.rerank("질문", candidates()))
                .isInstanceOfSatisfying(ChunkRerankerUnavailableException.class,
                        exception -> assertThat(exception.reason()).isEqualTo("INVALID_RESPONSE"));
    }

    @Test
    void 로컬_분당_제한을_넘으면_추가_API_요청을_차단한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.cohere.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CohereChunkReranker reranker = reranker(builder.build(), 1);
        server.expect(requestTo("https://api.cohere.test/v2/rerank"))
                .andRespond(withSuccess("{" + "\"results\":[{\"index\":0,\"relevance_score\":0.8}]}",
                        MediaType.APPLICATION_JSON));
        reranker.rerank("첫 질문", candidates());

        assertThatThrownBy(() -> reranker.rerank("다른 질문", candidates()))
                .isInstanceOfSatisfying(ChunkRerankerUnavailableException.class,
                        exception -> assertThat(exception.reason()).isEqualTo("LOCAL_RATE_LIMIT"));
        server.verify();
    }

    private CohereChunkReranker reranker(RestClient restClient, int maxRequestsPerMinute) {
        return new CohereChunkReranker(restClient, new ObjectMapper(), "rerank-v4.0-fast", 1024,
                Duration.ofMinutes(10), 100, maxRequestsPerMinute);
    }

    private List<ChunkRerankCandidate> candidates() {
        return List.of(
                new ChunkRerankCandidate("chunk-a", "문서 A", "경로/A.md", List.of("섹션 A"), "본문 A"),
                new ChunkRerankCandidate("chunk-b", "문서 B", "경로/B.md", List.of("섹션 B"), "본문 B")
        );
    }
}
