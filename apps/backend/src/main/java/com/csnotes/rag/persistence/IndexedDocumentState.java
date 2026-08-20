package com.csnotes.rag.persistence;

import java.util.List;

public record IndexedDocumentState(
        String documentId,
        String documentTitle,
        String documentPath,
        List<String> tags,
        String embeddingModel,
        List<IndexedChunkState> chunks
) {
    public record IndexedChunkState(int sequence, String contentHash, List<String> sectionPath) {
    }
}

