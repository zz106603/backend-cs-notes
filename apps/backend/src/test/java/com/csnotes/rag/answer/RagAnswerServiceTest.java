package com.csnotes.rag.answer;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingInput;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingPurpose;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkSearchResult;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.persistence.EmbeddedChunk;
import com.csnotes.rag.search.RagSearchService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagAnswerServiceTest {
    @Test
    void 검색된_청크를_번호가_있는_컨텍스트와_출처로_구성한다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RecordingVectorStore store = new RecordingVectorStore(List.of(result("트랜잭션 전파 본문", 0.81)));
        RagAnswerService service = service(generator, store, 2_000);

        RagAnswerResponse response = service.answer(new RagAnswerRequest("트랜잭션 전파란?", 3, 0.3));

        assertThat(generator.contexts.getFirst())
                .contains("[자료 1]", "백엔드/Spring/트랜잭션.md", "트랜잭션 전파 본문");
        assertThat(response.generated()).isTrue();
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().getFirst().number()).isEqualTo(1);
        assertThat(response.answer()).contains("[1]");
    }

    @Test
    void 검색_근거가_없으면_채팅_모델을_호출하지_않는다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RagAnswerService service = service(generator, new RecordingVectorStore(List.of()), 2_000);

        RagAnswerResponse response = service.answer(new RagAnswerRequest("없는 내용", null, null));

        assertThat(response.generated()).isFalse();
        assertThat(response.sources()).isEmpty();
        assertThat(generator.contexts).isEmpty();
    }

    @Test
    void 컨텍스트_문자_상한을_넘지_않도록_본문을_자른다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RecordingVectorStore store = new RecordingVectorStore(List.of(result("긴 본문 ".repeat(200), 0.9)));
        RagAnswerService service = service(generator, store, 180);

        RagAnswerResponse response = service.answer(new RagAnswerRequest("긴 문서 질문", null, null));

        assertThat(response.contextCharacters()).isLessThanOrEqualTo(180);
        assertThat(generator.contexts.getFirst()).hasSizeLessThanOrEqualTo(180);
    }

    @Test
    void 같은_질문과_출처의_답변을_캐시해_채팅_API_재호출을_막는다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RagAnswerService service = service(generator,
                new RecordingVectorStore(List.of(result("캐시 본문", 0.7))), 2_000);

        service.answer(new RagAnswerRequest("같은 질문", null, null));
        RagAnswerResponse second = service.answer(new RagAnswerRequest("같은 질문", null, null));

        assertThat(generator.contexts).hasSize(1);
        assertThat(second.cached()).isTrue();
    }

    @Test
    void 참고_자료_개수_한도를_검색과_API_호출_전에_검증한다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RagAnswerService service = service(generator, new RecordingVectorStore(List.of()), 2_000);

        assertThatThrownBy(() -> service.answer(new RagAnswerRequest("질문", 7, null)))
                .isInstanceOf(RagAnswerValidationException.class);
        assertThat(generator.contexts).isEmpty();
    }

    private RagAnswerService service(
            RecordingAnswerGenerator generator,
            RecordingVectorStore store,
            int maxContextCharacters
    ) {
        RagSearchService searchService = new RagSearchService(
                new TestEmbeddingProvider(), store, 5, 20, 500, 0.3, Duration.ofMinutes(10), 100
        );
        return new RagAnswerService(searchService, generator, 4, 6, maxContextCharacters,
                0.3, Duration.ofMinutes(10), 100);
    }

    private ChunkSearchResult result(String content, double score) {
        DocumentChunk chunk = new DocumentChunk("chunk-1", "doc-1", "트랜잭션",
                "백엔드/Spring/트랜잭션.md", List.of("Spring"), List.of("트랜잭션", "전파"),
                0, content, "a".repeat(64));
        return new ChunkSearchResult(chunk, score);
    }

    private static final class RecordingAnswerGenerator implements RagAnswerGenerator {
        private final List<String> contexts = new ArrayList<>();

        @Override public String modelName() { return "test-chat"; }

        @Override
        public GeneratedAnswer generate(String question, String groundedContext) {
            contexts.add(groundedContext);
            return new GeneratedAnswer("근거 기반 답변입니다. [1]", new RagAnswerUsage(100, 20, 120));
        }
    }

    private static final class TestEmbeddingProvider implements EmbeddingProvider {
        @Override public String modelName() { return "test-embedding"; }
        @Override public int dimensions() { return 3; }

        @Override
        public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
            return inputs.stream().map(input ->
                    new EmbeddingVector(input.id(), modelName(), new float[]{1, 0, 0})).toList();
        }
    }

    private static final class RecordingVectorStore implements ChunkVectorStore {
        private final List<ChunkSearchResult> results;

        private RecordingVectorStore(List<ChunkSearchResult> results) { this.results = results; }
        @Override public void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks) { }
        @Override public void deleteDocument(String documentId) { }
        @Override public Map<String, float[]> findReusableEmbeddings(String modelName, Set<String> contentHashes) { return Map.of(); }
        @Override public Set<String> findIndexedDocumentIds() { return Set.of(); }
        @Override public List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore) { return results; }
    }
}
