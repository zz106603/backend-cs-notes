package com.csnotes.rag.chunk;

import java.util.List;

/** 임베딩과 벡터 저장소에 전달되는 최소 검색 단위다. */
public record DocumentChunk(
        String id,
        String documentId,
        String documentTitle,
        String documentPath,
        List<String> tags,
        List<String> sectionPath,
        int sequence,
        String content,
        String contentHash
) {
    public DocumentChunk {
        tags = List.copyOf(tags);
        sectionPath = List.copyOf(sectionPath);
    }
}
