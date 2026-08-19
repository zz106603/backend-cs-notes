package com.csnotes.rag.indexing;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentService;
import com.csnotes.rag.chunk.HeadingAwareMarkdownChunker;
import com.csnotes.rag.embedding.EmbeddingInput;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingPurpose;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkSearchResult;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.persistence.EmbeddedChunk;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagIndexingServiceTest {
    @Test
    void 미리보기는_OpenAI와_DB를_변경하지_않고_예상_비용을_반환한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        RagIndexingService service = service(documentService("새로운 본문입니다."), provider, store, 10_000);

        RagIndexingResult result = service.synchronize(true);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.embeddedChunkCount()).isEqualTo(1);
        assertThat(result.embeddingCharacterCount()).isPositive();
        assertThat(provider.inputs).isEmpty();
        assertThat(store.replaced).isEmpty();
    }

    @Test
    void 같은_본문_해시의_벡터를_재사용해_API_호출을_생략한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        var documentService = documentService("변경되지 않은 본문입니다.");
        var chunker = new HeadingAwareMarkdownChunker(2000);
        var detail = documentService.findDocument("doc-1").orElseThrow();
        var chunk = chunker.chunk(new com.csnotes.rag.chunk.ChunkSourceDocument(
                detail.id(), detail.title(), detail.path(), detail.tags(), detail.content())).getFirst();
        store.reusable.put(chunk.contentHash(), new float[]{1, 0, 0});
        RagIndexingService service = new RagIndexingService(
                documentService, chunker, provider, store, 10, 10, 10_000);

        RagIndexingResult result = service.synchronize(false);

        assertThat(result.reusedChunkCount()).isEqualTo(1);
        assertThat(result.embeddedChunkCount()).isZero();
        assertThat(provider.inputs).isEmpty();
        assertThat(store.replaced).hasSize(1);
    }

    @Test
    void 새_청크만_임베딩하고_삭제된_문서의_벡터를_정리한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        store.indexedDocumentIds.add("deleted-doc");
        RagIndexingService service = service(documentService("새로운 본문입니다."), provider, store, 10_000);

        RagIndexingResult result = service.synchronize(false);

        assertThat(provider.inputs).hasSize(1);
        assertThat(store.replaced).hasSize(1);
        assertThat(store.deleted).containsExactly("deleted-doc");
        assertThat(result.deletedDocumentCount()).isEqualTo(1);
    }

    @Test
    void 실행당_임베딩_문자_한도를_넘으면_API_호출_전에_중단한다() {
        RecordingEmbeddingProvider provider = new RecordingEmbeddingProvider();
        RecordingVectorStore store = new RecordingVectorStore();
        RagIndexingService service = service(documentService("비용 한도를 넘는 본문입니다."), provider, store, 5);

        assertThatThrownBy(() -> service.synchronize(false))
                .isInstanceOf(RagIndexingLimitException.class)
                .hasMessageContaining("max-characters-per-run");
        assertThat(provider.inputs).isEmpty();
        assertThat(store.replaced).isEmpty();
    }

    private RagIndexingService service(
            DocumentService documentService,
            RecordingEmbeddingProvider provider,
            RecordingVectorStore store,
            long maxCharacters
    ) {
        return new RagIndexingService(documentService, new HeadingAwareMarkdownChunker(2000),
                provider, store, 10, 10, maxCharacters);
    }

    private DocumentService documentService(String content) {
        DocumentService service = mock(DocumentService.class);
        var summary = new DocumentModels.DocumentSummaryResponse(
                "doc-1", "테스트", "백엔드", "백엔드/테스트.md", Instant.EPOCH, List.of("RAG"), null);
        var detail = new DocumentModels.DocumentDetailResponse(
                "doc-1", "테스트", "백엔드", "백엔드/테스트.md", content, Instant.EPOCH, List.of("RAG"));
        when(service.findDocuments(null, null)).thenReturn(List.of(summary));
        when(service.findDocument("doc-1")).thenReturn(java.util.Optional.of(detail));
        return service;
    }

    private static final class RecordingEmbeddingProvider implements EmbeddingProvider {
        private final List<EmbeddingInput> inputs = new ArrayList<>();

        @Override public String modelName() { return "test-model"; }
        @Override public int dimensions() { return 3; }

        @Override
        public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
            this.inputs.addAll(inputs);
            return inputs.stream().map(input ->
                    new EmbeddingVector(input.id(), modelName(), new float[]{1, 0, 0})).toList();
        }
    }

    private static final class RecordingVectorStore implements ChunkVectorStore {
        private final Map<String, float[]> reusable = new HashMap<>();
        private final Set<String> indexedDocumentIds = new HashSet<>();
        private final List<List<EmbeddedChunk>> replaced = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();

        @Override public void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks) { replaced.add(chunks); }
        @Override public void deleteDocument(String documentId) { deleted.add(documentId); }
        @Override public Map<String, float[]> findReusableEmbeddings(String modelName, Set<String> hashes) {
            return Map.copyOf(reusable);
        }
        @Override public Set<String> findIndexedDocumentIds() { return Set.copyOf(indexedDocumentIds); }
        @Override public List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore) { return List.of(); }
    }
}
