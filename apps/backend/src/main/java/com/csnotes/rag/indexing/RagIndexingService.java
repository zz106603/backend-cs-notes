package com.csnotes.rag.indexing;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentService;
import com.csnotes.rag.chunk.ChunkSourceDocument;
import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.chunk.MarkdownChunker;
import com.csnotes.rag.embedding.EmbeddingInput;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.persistence.EmbeddedChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Markdown 문서를 변경분만 임베딩해 pgvector와 동기화하는 M4.5 색인 유스케이스다. */
public final class RagIndexingService {
    private final DocumentService documentService;
    private final MarkdownChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkVectorStore vectorStore;
    private final int maxDocuments;
    private final int maxChunksPerDocument;
    private final long maxCharactersPerRun;
    private final AtomicBoolean indexing = new AtomicBoolean();

    public RagIndexingService(
            DocumentService documentService,
            MarkdownChunker chunker,
            EmbeddingProvider embeddingProvider,
            ChunkVectorStore vectorStore,
            int maxDocuments,
            int maxChunksPerDocument,
            long maxCharactersPerRun
    ) {
        this.documentService = documentService;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.maxDocuments = maxDocuments;
        this.maxChunksPerDocument = maxChunksPerDocument;
        this.maxCharactersPerRun = maxCharactersPerRun;
    }

    /** dry-run을 기본 진입점으로 두고 실제 실행 전에도 동일한 비용 한도를 검증한다. */
    public RagIndexingResult synchronize(boolean dryRun) {
        if (!indexing.compareAndSet(false, true)) {
            throw new IllegalStateException("RAG indexing is already running");
        }
        try {
            return doSynchronize(dryRun);
        } finally {
            indexing.set(false);
        }
    }

    private RagIndexingResult doSynchronize(boolean dryRun) {
        List<DocumentModels.DocumentSummaryResponse> summaries = documentService.findDocuments(null, null);
        enforceLimit(summaries.size() <= maxDocuments,
                "Document count exceeds rag.indexing.max-documents: " + summaries.size());

        Set<String> currentDocumentIds = new HashSet<>();
        List<IndexPlan> plans = new ArrayList<>();
        int chunkCount = 0;
        int reusedCount = 0;
        long embeddingCharacters = 0;

        for (DocumentModels.DocumentSummaryResponse summary : summaries) {
            DocumentModels.DocumentDetailResponse detail = documentService.findDocument(summary.id())
                    .orElseThrow(() -> new IllegalStateException("Document disappeared during indexing: " + summary.id()));
            currentDocumentIds.add(detail.id());
            List<DocumentChunk> chunks = chunker.chunk(new ChunkSourceDocument(
                    detail.id(), detail.title(), detail.path(), detail.tags(), detail.content()
            ));
            enforceLimit(chunks.size() <= maxChunksPerDocument,
                    "Chunk count exceeds rag.indexing.max-chunks-per-document: " + detail.path());

            Set<String> hashes = chunks.stream().map(DocumentChunk::contentHash).collect(java.util.stream.Collectors.toSet());
            Map<String, float[]> reusable = vectorStore.findReusableEmbeddings(
                    embeddingProvider.modelName(), hashes
            );
            List<DocumentChunk> missing = chunks.stream()
                    .filter(chunk -> !reusable.containsKey(chunk.contentHash()))
                    .toList();
            long missingCharacters = missing.stream().mapToLong(chunk -> chunk.content().length()).sum();

            chunkCount += chunks.size();
            reusedCount += chunks.size() - missing.size();
            embeddingCharacters += missingCharacters;
            enforceLimit(embeddingCharacters <= maxCharactersPerRun,
                    "Embedding input exceeds rag.indexing.max-characters-per-run: " + embeddingCharacters);
            plans.add(new IndexPlan(detail.id(), chunks, reusable, missing));
        }

        Set<String> deleted = new HashSet<>(vectorStore.findIndexedDocumentIds());
        deleted.removeAll(currentDocumentIds);
        if (!dryRun) {
            for (IndexPlan plan : plans) execute(plan);
            deleted.forEach(vectorStore::deleteDocument);
        }
        return new RagIndexingResult(dryRun, summaries.size(), chunkCount,
                chunkCount - reusedCount, reusedCount, deleted.size(), embeddingCharacters);
    }

    private void execute(IndexPlan plan) {
        Map<String, EmbeddingVector> newlyEmbedded = new LinkedHashMap<>();
        if (!plan.missing().isEmpty()) {
            List<EmbeddingVector> vectors = embeddingProvider.embedDocuments(plan.missing().stream()
                    .map(chunk -> new EmbeddingInput(chunk.id(), chunk.content()))
                    .toList());
            vectors.forEach(vector -> newlyEmbedded.put(vector.inputId(), vector));
        }

        List<EmbeddedChunk> embeddedChunks = plan.chunks().stream().map(chunk -> {
            EmbeddingVector vector = newlyEmbedded.get(chunk.id());
            if (vector == null) {
                vector = new EmbeddingVector(chunk.id(), embeddingProvider.modelName(),
                        plan.reusable().get(chunk.contentHash()));
            }
            return new EmbeddedChunk(chunk, vector);
        }).toList();
        vectorStore.replaceDocumentChunks(plan.documentId(), embeddedChunks);
    }

    private void enforceLimit(boolean allowed, String message) {
        if (!allowed) throw new RagIndexingLimitException(message);
    }

    private record IndexPlan(
            String documentId,
            List<DocumentChunk> chunks,
            Map<String, float[]> reusable,
            List<DocumentChunk> missing
    ) {
    }
}
