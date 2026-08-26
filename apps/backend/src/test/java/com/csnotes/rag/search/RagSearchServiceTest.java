package com.csnotes.rag.search;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingInput;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingPurpose;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkSearchResult;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.persistence.EmbeddedChunk;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagSearchServiceTest {
    @Test
    void 질의를_임베딩하고_유사도_순서의_청크와_출처를_반환한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        store.results = List.of(result("트랜잭션 전파", 0.91), result("격리 수준", 0.82));
        RagSearchService service = service(provider, store);

        RagSearchResponse response = service.search(
                new RagSearchRequest("트랜잭션이란?", 2, 0.7, RagSearchMode.DENSE));

        assertThat(provider.queries).containsExactly("트랜잭션이란?");
        assertThat(store.limit).isEqualTo(2);
        assertThat(store.minimumScore).isEqualTo(0.7);
        assertThat(response.results()).extracting(RagSearchHit::score).containsExactly(0.91, 0.82);
        assertThat(response.results().getFirst().documentPath()).isEqualTo("백엔드/트랜잭션.md");
        assertThat(response.cachedQueryEmbedding()).isFalse();
    }

    @Test
    void 같은_질의의_임베딩을_캐시해_OpenAI_재호출을_막는다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        RagSearchService service = service(provider, store);

        service.search(new RagSearchRequest("동일 질의", null, null, RagSearchMode.DENSE));
        RagSearchResponse second = service.search(
                new RagSearchRequest("동일 질의", null, null, RagSearchMode.DENSE));

        assertThat(provider.queries).containsExactly("동일 질의");
        assertThat(second.cachedQueryEmbedding()).isTrue();
    }

    @Test
    void 희소_검색은_임베딩_API를_호출하지_않고_FTS_결과를_반환한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        store.results = List.of(result("REQUIRES_NEW 전파", 0.74));
        RagSearchService service = service(provider, store);

        RagSearchResponse response = service.search(
                new RagSearchRequest("REQUIRES_NEW", 3, null, RagSearchMode.SPARSE));

        assertThat(provider.queries).isEmpty();
        assertThat(store.sparseQuery).isEqualTo("REQUIRES_NEW");
        assertThat(store.limit).isEqualTo(3);
        assertThat(store.minimumScore).isZero();
        assertThat(response.mode()).isEqualTo(RagSearchMode.SPARSE);
        assertThat(response.embeddingModel()).isNull();
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void 검색어와_결과_개수와_최소_유사도를_API_호출_전에_검증한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        RagSearchService service = service(provider, store);

        assertThatThrownBy(() -> service.search(new RagSearchRequest(" ", null, null, null)))
                .isInstanceOf(RagSearchValidationException.class);
        assertThatThrownBy(() -> service.search(new RagSearchRequest("질의", 21, null, null)))
                .isInstanceOf(RagSearchValidationException.class);
        assertThatThrownBy(() -> service.search(new RagSearchRequest("질의", null, 1.1, null)))
                .isInstanceOf(RagSearchValidationException.class);
        assertThat(provider.queries).isEmpty();
    }

    private RagSearchService service(RecordingEmbeddingProvider provider, RecordingVectorStore store) {
        return new RagSearchService(provider, store, 5, 20, 500, 0.5, 0.0, Duration.ofMinutes(10), 100);
    }

    private ChunkSearchResult result(String content, double score) {
        var chunk = new DocumentChunk("chunk-" + content, "doc-1", "트랜잭션", "백엔드/트랜잭션.md",
                List.of("Spring"), List.of("트랜잭션"), 0, content, "a".repeat(64));
        return new ChunkSearchResult(chunk, score);
    }

    private static final class RecordingEmbeddingProvider implements EmbeddingProvider {
        private final List<String> queries = new ArrayList<>();

        @Override public String modelName() { return "test-model"; }
        @Override public int dimensions() { return 3; }

        @Override
        public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
            inputs.forEach(input -> queries.add(input.text()));
            return inputs.stream().map(input ->
                    new EmbeddingVector(input.id(), modelName(), new float[]{1, 0, 0})).toList();
        }
    }

    private static final class RecordingVectorStore implements ChunkVectorStore {
        private List<ChunkSearchResult> results = List.of();
        private int limit;
        private double minimumScore;
        private String sparseQuery;

        @Override public void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks) { }
        @Override public void deleteDocument(String documentId) { }
        @Override public Map<String, float[]> findReusableEmbeddings(String modelName, Set<String> contentHashes) { return Map.of(); }
        @Override public Set<String> findIndexedDocumentIds() { return Set.of(); }

        @Override
        public List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore) {
            this.limit = limit;
            this.minimumScore = minimumScore;
            return results;
        }

        @Override
        public List<ChunkSearchResult> searchSparse(String query, int limit, double minimumScore) {
            this.sparseQuery = query;
            this.limit = limit;
            this.minimumScore = minimumScore;
            return results;
        }
    }
}
