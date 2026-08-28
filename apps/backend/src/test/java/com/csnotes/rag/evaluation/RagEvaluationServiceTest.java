package com.csnotes.rag.evaluation;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingInput;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingPurpose;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkSearchResult;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.persistence.EmbeddedChunk;
import com.csnotes.rag.search.RagSearchMode;
import com.csnotes.rag.search.RagSearchService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagEvaluationServiceTest {
    @Test
    void 세_검색_방식의_기대_문서_순위와_재현율을_비교한다() {
        RecordingRepository repository = new RecordingRepository();
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        vectorStore.denseResults = List.of(result("dense-a", "A.md", 0.9), result("dense-b", "B.md", 0.8));
        vectorStore.sparseResults = List.of(result("sparse-c", "C.md", 0.7), result("dense-a", "A.md", 0.6));
        RagEvaluationService service = service(repository, vectorStore);
        RagEvaluationCase evaluationCase = service.create(
                new CreateRagEvaluationCaseRequest("트랜잭션 질문", List.of("C.md")));

        RagEvaluationRunResponse response = service.run(evaluationCase.id());

        assertThat(response.modes()).extracting(RagEvaluationModeResult::mode)
                .containsExactly(RagSearchMode.DENSE, RagSearchMode.SPARSE, RagSearchMode.HYBRID);
        assertThat(response.modes().get(0).recallAtLimit()).isZero();
        assertThat(response.modes().get(0).firstRelevantRank()).isNull();
        assertThat(response.modes().get(1).firstRelevantRank()).isEqualTo(1);
        assertThat(response.modes().get(1).reciprocalRank()).isEqualTo(1.0);
        assertThat(response.modes().get(2).firstRelevantRank()).isEqualTo(2);
        assertThat(response.modes().get(2).recallAtLimit()).isEqualTo(1.0);
    }

    @Test
    void 평가_질문과_기대_문서를_검증하고_중복_경로를_제거한다() {
        RecordingRepository repository = new RecordingRepository();
        RagEvaluationService service = service(repository, new RecordingVectorStore());

        assertThatThrownBy(() -> service.create(new CreateRagEvaluationCaseRequest(" ", List.of("A.md"))))
                .isInstanceOf(RagEvaluationValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateRagEvaluationCaseRequest("질문", List.of())))
                .isInstanceOf(RagEvaluationValidationException.class);

        RagEvaluationCase saved = service.create(
                new CreateRagEvaluationCaseRequest(" 질문 ", List.of(" A.md ", "A.md", "B.md")));
        assertThat(saved.query()).isEqualTo("질문");
        assertThat(saved.expectedDocumentPaths()).containsExactly("A.md", "B.md");
    }

    private RagEvaluationService service(RecordingRepository repository, RecordingVectorStore vectorStore) {
        RagSearchService searchService = new RagSearchService(new TestEmbeddingProvider(), vectorStore,
                5, 20, 500, 0.5, 0.0, 20, 60, Duration.ofMinutes(10), 100);
        return new RagEvaluationService(repository, searchService, 10,
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));
    }

    private ChunkSearchResult result(String chunkId, String path, double score) {
        DocumentChunk chunk = new DocumentChunk(chunkId, "doc-" + path, path, path,
                List.of(), List.of("section"), 0, chunkId, "a".repeat(64));
        return new ChunkSearchResult(chunk, score);
    }

    private static final class RecordingRepository implements RagEvaluationRepository {
        private final Map<UUID, RagEvaluationCase> cases = new LinkedHashMap<>();

        @Override public List<RagEvaluationCase> findAll() { return new ArrayList<>(cases.values()); }
        @Override public Optional<RagEvaluationCase> findById(UUID id) { return Optional.ofNullable(cases.get(id)); }
        @Override public void save(RagEvaluationCase evaluationCase) { cases.put(evaluationCase.id(), evaluationCase); }
        @Override public void deleteById(UUID id) { cases.remove(id); }
    }

    private static final class TestEmbeddingProvider implements EmbeddingProvider {
        @Override public String modelName() { return "test-model"; }
        @Override public int dimensions() { return 3; }
        @Override public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
            return inputs.stream().map(input ->
                    new EmbeddingVector(input.id(), modelName(), new float[]{1, 0, 0})).toList();
        }
    }

    private static final class RecordingVectorStore implements ChunkVectorStore {
        private List<ChunkSearchResult> denseResults = List.of();
        private List<ChunkSearchResult> sparseResults = List.of();

        @Override public void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks) { }
        @Override public void deleteDocument(String documentId) { }
        @Override public Map<String, float[]> findReusableEmbeddings(String modelName, Set<String> hashes) { return Map.of(); }
        @Override public Set<String> findIndexedDocumentIds() { return Set.of(); }
        @Override public List<ChunkSearchResult> search(EmbeddingVector query, int limit, double score) { return denseResults; }
        @Override public List<ChunkSearchResult> searchSparse(String query, int limit, double score) { return sparseResults; }
    }
}
