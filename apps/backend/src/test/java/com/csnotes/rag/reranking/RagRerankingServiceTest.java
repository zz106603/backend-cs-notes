package com.csnotes.rag.reranking;

import com.csnotes.rag.search.RagSearchHit;
import com.csnotes.rag.search.RagSearchMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagRerankingServiceTest {
    @Test
    void 비활성화하면_RRF_순서를_유지하고_요청한_개수만_반환한다() {
        RagRerankingService service = RagRerankingService.disabled();

        List<RagSearchHit> results = service.rerank(
                "질문", List.of(hit("a"), hit("b"), hit("c")), 2);

        assertThat(results).extracting(RagSearchHit::chunkId).containsExactly("a", "b");
        assertThat(results).allSatisfy(hit -> {
            assertThat(hit.rerankScore()).isNull();
            assertThat(hit.rerankRank()).isNull();
        });
    }

    @Test
    void 모델_점수로_후보를_재정렬하고_최소_점수보다_낮은_후보를_제외한다() {
        RecordingReranker reranker = new RecordingReranker(List.of(
                new ChunkRerankScore("b", 0.91),
                new ChunkRerankScore("a", 0.72),
                new ChunkRerankScore("c", 0.20)
        ));
        RagRerankingService service = new RagRerankingService(true, reranker, 0.5);

        List<RagSearchHit> results = service.rerank(
                "트랜잭션 질문", List.of(hit("a"), hit("b"), hit("c")), 3);

        assertThat(reranker.query).isEqualTo("트랜잭션 질문");
        assertThat(reranker.candidates).extracting(ChunkRerankCandidate::chunkId)
                .containsExactly("a", "b", "c");
        assertThat(results).extracting(RagSearchHit::chunkId).containsExactly("b", "a");
        assertThat(results).extracting(RagSearchHit::rerankScore).containsExactly(0.91, 0.72);
        assertThat(results).extracting(RagSearchHit::rerankRank).containsExactly(1, 2);
    }

    @Test
    void 모델이_후보에_없는_Chunk를_반환하면_검색_결과로_사용하지_않는다() {
        RagRerankingService service = new RagRerankingService(true,
                new RecordingReranker(List.of(new ChunkRerankScore("unknown", 0.9))), 0.0);

        assertThatThrownBy(() -> service.rerank("질문", List.of(hit("a")), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    private RagSearchHit hit(String chunkId) {
        return new RagSearchHit(chunkId, "doc-" + chunkId, "문서 " + chunkId, "경로/" + chunkId + ".md",
                List.of(), List.of("섹션"), "본문 " + chunkId, 0.5,
                0.5, null, 1, null, null, null, List.of(RagSearchMode.DENSE));
    }

    private static final class RecordingReranker implements ChunkReranker {
        private final List<ChunkRerankScore> scores;
        private String query;
        private List<ChunkRerankCandidate> candidates = new ArrayList<>();

        private RecordingReranker(List<ChunkRerankScore> scores) {
            this.scores = scores;
        }

        @Override public String modelName() { return "test-reranker"; }

        @Override
        public List<ChunkRerankScore> rerank(String query, List<ChunkRerankCandidate> candidates) {
            this.query = query;
            this.candidates = List.copyOf(candidates);
            return scores;
        }
    }
}
