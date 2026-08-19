package com.csnotes.rag.chunk;

import java.util.List;

/** 문서 분할 정책을 교체할 수 있게 만드는 RAG 청킹 경계다. */
public interface MarkdownChunker {
    List<DocumentChunk> chunk(ChunkSourceDocument document);
}
