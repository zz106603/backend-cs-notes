package com.csnotes.rag.indexing;

public record RagIndexingResult(
        boolean dryRun,
        int documentCount,
        int chunkCount,
        int embeddedChunkCount,
        int reusedChunkCount,
        int deletedDocumentCount,
        long embeddingCharacterCount
) {
}
