package com.csnotes.rag.persistence;

import com.csnotes.rag.embedding.EmbeddingVector;

import java.util.List;

/** Chunk 저장 기술을 RAG 동기화와 검색 로직에서 분리하는 포트다. */
public interface ChunkVectorStore {
    void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks);

    void deleteDocument(String documentId);

    List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore);
}
