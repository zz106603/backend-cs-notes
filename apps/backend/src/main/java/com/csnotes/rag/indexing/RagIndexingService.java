package com.csnotes.rag.indexing;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentService;
import com.csnotes.document.metadata.DocumentMetadata;
import com.csnotes.document.metadata.DocumentMetadataRepository;
import com.csnotes.rag.chunk.ChunkSourceDocument;
import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.chunk.MarkdownChunker;
import com.csnotes.rag.embedding.EmbeddingInput;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.csnotes.rag.persistence.ChunkVectorStore;
import com.csnotes.rag.persistence.EmbeddedChunk;
import com.csnotes.rag.persistence.IndexedDocumentState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

/** Markdown 문서를 변경분만 임베딩해 pgvector와 동기화하는 M4.5 색인 유스케이스다. */
public final class RagIndexingService {
    private final DocumentService documentService;
    private final MarkdownChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkVectorStore vectorStore;
    private final DocumentMetadataRepository metadataRepository;
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
        this(documentService, chunker, embeddingProvider, vectorStore, null,
                maxDocuments, maxChunksPerDocument, maxCharactersPerRun);
    }

    public RagIndexingService(
            DocumentService documentService,
            MarkdownChunker chunker,
            EmbeddingProvider embeddingProvider,
            ChunkVectorStore vectorStore,
            DocumentMetadataRepository metadataRepository,
            int maxDocuments,
            int maxChunksPerDocument,
            long maxCharactersPerRun
    ) {
        this.documentService = documentService;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.metadataRepository = metadataRepository;
        this.maxDocuments = maxDocuments;
        this.maxChunksPerDocument = maxChunksPerDocument;
        this.maxCharactersPerRun = maxCharactersPerRun;
    }

    /** dry-run을 기본 진입점으로 두고 실제 실행 전에도 동일한 비용 한도를 검증한다. */
    public RagIndexingResult synchronize(boolean dryRun) {
        return synchronize(null, dryRun);
    }

    /** 인증 사용자는 자신의 활성 문서 메타데이터와 연결된 Chunk만 색인할 수 있다. */
    public RagIndexingResult synchronize(UUID ownerId, boolean dryRun) {
        if (!indexing.compareAndSet(false, true)) {
            throw new IllegalStateException("RAG indexing is already running");
        }
        try {
            return doSynchronize(ownerId, dryRun);
        } finally {
            indexing.set(false);
        }
    }

    private RagIndexingResult doSynchronize(UUID ownerId, boolean dryRun) {
        List<DocumentModels.DocumentSummaryResponse> summaries = documentService.findDocuments(null, null);
        enforceLimit(summaries.size() <= maxDocuments,
                "Document count exceeds rag.indexing.max-documents: " + summaries.size());

        Set<String> currentDocumentIds = new HashSet<>();
        Map<String, IndexedDocumentState> indexedDocuments = vectorStore.findIndexedDocumentStates();
        Map<String, DocumentMetadata> metadataBySourceId = metadataBySourceId(ownerId);
        List<IndexPlan> plans = new ArrayList<>();
        List<RagIndexingDocumentResult> documentResults = new ArrayList<>();
        int chunkCount = 0;
        int reusedCount = 0;
        long embeddingCharacters = 0;

        for (DocumentModels.DocumentSummaryResponse summary : summaries) {
            DocumentModels.DocumentDetailResponse detail = documentService.findDocument(summary.id())
                    .orElseThrow(() -> new IllegalStateException("Document disappeared during indexing: " + summary.id()));
            currentDocumentIds.add(detail.id());
            UUID documentMetadataId = ownerId == null ? null : requireMetadata(metadataBySourceId, detail).id();
            List<DocumentChunk> chunks = chunker.chunk(new ChunkSourceDocument(
                    detail.id(), detail.title(), detail.path(), detail.tags(), detail.content()
            ));
            enforceLimit(chunks.size() <= maxChunksPerDocument,
                    "Chunk count exceeds rag.indexing.max-chunks-per-document: " + detail.path());

            IndexedDocumentState indexed = indexedDocuments.get(detail.id());
            String action = indexed == null ? "NEW" : matches(indexed, detail, chunks) ? "UNCHANGED" : "UPDATED";
            Map<String, float[]> reusable;
            List<DocumentChunk> missing;
            if (action.equals("UNCHANGED")) {
                reusable = Map.of();
                missing = List.of();
            } else {
                Set<String> hashes = chunks.stream().map(DocumentChunk::contentHash)
                        .collect(java.util.stream.Collectors.toSet());
                reusable = vectorStore.findReusableEmbeddings(embeddingProvider.modelName(), hashes);
                missing = chunks.stream()
                        .filter(chunk -> !reusable.containsKey(chunk.contentHash()))
                        .toList();
            }
            long missingCharacters = missing.stream().mapToLong(chunk -> chunk.content().length()).sum();
            int documentReusedCount = action.equals("UNCHANGED") ? chunks.size() : chunks.size() - missing.size();

            chunkCount += chunks.size();
            reusedCount += documentReusedCount;
            embeddingCharacters += missingCharacters;
            enforceLimit(embeddingCharacters <= maxCharactersPerRun,
                    "Embedding input exceeds rag.indexing.max-characters-per-run: " + embeddingCharacters);
            plans.add(new IndexPlan(documentMetadataId, detail.id(), chunks, reusable, missing, action));
            documentResults.add(new RagIndexingDocumentResult(
                    detail.id(), detail.title(), detail.path(), action, chunks.size(), missing.size(),
                    documentReusedCount, missingCharacters
            ));
        }

        Set<String> deleted = new HashSet<>(vectorStore.findIndexedDocumentIds());
        deleted.removeAll(currentDocumentIds);
        for (String deletedDocumentId : deleted) {
            IndexedDocumentState state = indexedDocuments.get(deletedDocumentId);
            documentResults.add(new RagIndexingDocumentResult(
                    deletedDocumentId,
                    state == null ? "삭제된 문서" : state.documentTitle(),
                    state == null ? "" : state.documentPath(),
                    "DELETED",
                    state == null ? 0 : state.chunks().size(),
                    0, 0, 0
            ));
        }
        if (!dryRun) {
            plans.stream().filter(plan -> !plan.action().equals("UNCHANGED")).forEach(this::execute);
            deleted.forEach(vectorStore::deleteDocument);
        }
        int changedDocumentCount = (int) documentResults.stream()
                .filter(document -> !document.action().equals("UNCHANGED"))
                .count();
        int unchangedDocumentCount = (int) documentResults.stream()
                .filter(document -> document.action().equals("UNCHANGED"))
                .count();
        return new RagIndexingResult(dryRun, embeddingProvider.modelName(), summaries.size(),
                changedDocumentCount, unchangedDocumentCount,
                chunkCount, chunkCount - reusedCount, reusedCount, deleted.size(), embeddingCharacters,
                List.copyOf(documentResults));
    }

    private boolean matches(
            IndexedDocumentState indexed,
            DocumentModels.DocumentDetailResponse detail,
            List<DocumentChunk> chunks
    ) {
        if (!indexed.documentTitle().equals(detail.title())
                || !indexed.documentPath().equals(detail.path())
                || !indexed.tags().equals(detail.tags())
                || !indexed.embeddingModel().equals(embeddingProvider.modelName())
                || indexed.chunks().size() != chunks.size()) {
            return false;
        }
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            IndexedDocumentState.IndexedChunkState stored = indexed.chunks().get(index);
            if (stored.sequence() != chunk.sequence()
                    || !stored.contentHash().equals(chunk.contentHash())
                    || !stored.sectionPath().equals(chunk.sectionPath())) {
                return false;
            }
        }
        return true;
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
        vectorStore.replaceDocumentChunks(plan.documentMetadataId(), plan.documentId(), embeddedChunks);
    }

    private Map<String, DocumentMetadata> metadataBySourceId(UUID ownerId) {
        if (ownerId == null) return Map.of();
        if (metadataRepository == null) {
            throw new IllegalStateException("문서 메타데이터 저장소가 활성화되어 있지 않습니다.");
        }
        return metadataRepository.findActiveByOwnerIdIndexedBySourceId(ownerId);
    }

    private DocumentMetadata requireMetadata(
            Map<String, DocumentMetadata> metadataBySourceId,
            DocumentModels.DocumentDetailResponse document
    ) {
        DocumentMetadata metadata = metadataBySourceId.get(document.id());
        if (metadata == null) {
            throw new IllegalStateException(
                    "문서 메타데이터가 없습니다. 먼저 내 문서로 동기화해 주세요: " + document.path());
        }
        return metadata;
    }

    private void enforceLimit(boolean allowed, String message) {
        if (!allowed) throw new RagIndexingLimitException(message);
    }

    private record IndexPlan(
            UUID documentMetadataId,
            String documentId,
            List<DocumentChunk> chunks,
            Map<String, float[]> reusable,
            List<DocumentChunk> missing,
            String action
    ) {
    }
}
