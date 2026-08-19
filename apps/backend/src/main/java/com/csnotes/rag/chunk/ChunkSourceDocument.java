package com.csnotes.rag.chunk;

import java.util.List;

/** Markdown 원문과 검색에 필요한 문서 메타데이터를 청킹 단계로 전달한다. */
public record ChunkSourceDocument(
        String documentId,
        String title,
        String path,
        List<String> tags,
        String content
) {
    public ChunkSourceDocument {
        tags = tags == null ? List.of() : List.copyOf(tags);
        content = content == null ? "" : content.replace("\r\n", "\n");
    }
}
