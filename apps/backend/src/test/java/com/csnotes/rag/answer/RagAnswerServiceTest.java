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
import com.csnotes.rag.reranking.RagRerankingService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.math.BigDecimal;
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

    @Test
    void 질문_길이는_백엔드에서도_제한한다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RagAnswerService service = service(generator, new RecordingVectorStore(List.of()), 2_000);

        assertThatThrownBy(() -> service.answer(new RagAnswerRequest("가".repeat(501), null, null)))
                .isInstanceOf(RagAnswerValidationException.class);
        assertThat(generator.contexts).isEmpty();
    }

    @Test
    void 생성된_답변의_토큰과_예상_비용을_기록한다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RecordingUsageStore usageStore = new RecordingUsageStore();
        RagAnswerService service = service(generator,
                new RecordingVectorStore(List.of(result("비용 기록 본문", 0.8))), 2_000, usageStore);

        RagAnswerResponse response = service.answer(new RagAnswerRequest("비용은?", null, null));

        assertThat(response.estimatedCostUsd()).isEqualByComparingTo("0.00007200");
        assertThat(usageStore.records).singleElement().satisfies(record -> {
            assertThat(record.status()).isEqualTo("GENERATED");
            assertThat(record.questionHash()).hasSize(64);
            assertThat(record.usage().totalTokens()).isEqualTo(120);
        });
    }

    @Test
    void 다음_호출의_최대_예상_비용이_일일_한도를_넘으면_모델을_호출하지_않는다() {
        RecordingAnswerGenerator generator = new RecordingAnswerGenerator();
        RecordingUsageStore usageStore = new RecordingUsageStore();
        usageStore.totalCost = new BigDecimal("0.249");
        RagAnswerService service = service(generator,
                new RecordingVectorStore(List.of(result("한도 본문", 0.8))), 2_000, usageStore);

        assertThatThrownBy(() -> service.answer(new RagAnswerRequest("한도 질문", null, null)))
                .isInstanceOf(RagAnswerLimitExceededException.class);
        assertThat(generator.contexts).isEmpty();
    }

    private RagAnswerService service(
            RecordingAnswerGenerator generator,
            RecordingVectorStore store,
            int maxContextCharacters
    ) {
        return service(generator, store, maxContextCharacters, new RecordingUsageStore());
    }

    private RagAnswerService service(
            RecordingAnswerGenerator generator,
            RecordingVectorStore store,
            int maxContextCharacters,
            RecordingUsageStore usageStore
    ) {
        RagSearchService searchService = new RagSearchService(
                new TestEmbeddingProvider(), store, 5, 20, 500, 0.3, 0.0,
                20, 60, RagRerankingService.disabled(), Duration.ofMinutes(10), 100
        );
        return new RagAnswerService(searchService, generator, 4, 6, maxContextCharacters,
                0.3, Duration.ofMinutes(10), 100, usageStore,
                new RagAnswerCostPolicy(new BigDecimal("0.40"), new BigDecimal("1.60"),
                        new BigDecimal("0.25"), 600),
                Clock.fixed(Instant.parse("2026-08-20T03:00:00Z"), ZoneId.of("Asia/Seoul")));
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
        @Override public List<ChunkSearchResult> searchSparse(String query, int limit, double minimumScore) { return results; }
    }

    private static final class RecordingUsageStore implements RagAnswerUsageStore {
        private BigDecimal totalCost = BigDecimal.ZERO;
        private final List<RagAnswerUsageRecord> records = new ArrayList<>();

        @Override
        public BigDecimal totalCostBetween(Instant fromInclusive, Instant toExclusive) {
            return totalCost;
        }

        @Override
        public void save(RagAnswerUsageRecord record) {
            records.add(record);
        }
    }
}
