package com.csnotes.rag.persistence;

import com.csnotes.rag.embedding.EmbeddingVector;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Chunk 저장 기술을 RAG 동기화와 검색 로직에서 분리하는 포트다. */
public interface ChunkVectorStore {
    void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks);

    void deleteDocument(String documentId);

    /** 같은 모델과 본문 해시로 이미 계산한 벡터를 재사용해 외부 API 호출 비용을 줄인다. */
    Map<String, float[]> findReusableEmbeddings(String modelName, Set<String> contentHashes);

    Set<String> findIndexedDocumentIds();

    List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore);
}
