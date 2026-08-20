package com.csnotes.rag.indexing;

import java.util.List;

public record RagIndexingResult(
        boolean dryRun,
        String embeddingModel,
        int documentCount,
        int changedDocumentCount,
        int unchangedDocumentCount,
        int chunkCount,
        int embeddedChunkCount,
        int reusedChunkCount,
        int deletedDocumentCount,
        long embeddingCharacterCount,
        List<RagIndexingDocumentResult> documents
) {
}
