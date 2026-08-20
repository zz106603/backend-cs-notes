package com.csnotes.rag.indexing;

public record RagIndexingDocumentResult(
        String documentId,
        String documentTitle,
        String documentPath,
        String action,
        int chunkCount,
        int embeddedChunkCount,
        int reusedChunkCount,
        long embeddingCharacterCount
) {
}

