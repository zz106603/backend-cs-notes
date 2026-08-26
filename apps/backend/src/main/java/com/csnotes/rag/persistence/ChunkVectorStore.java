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

    /** 현재 파일과 저장된 Chunk 구성을 비교해 불필요한 문서 교체도 건너뛸 수 있게 한다. */
    default Map<String, IndexedDocumentState> findIndexedDocumentStates() {
        return Map.of();
    }

    List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore);

    /** PostgreSQL FTS로 정확한 용어가 포함된 Chunk를 검색한다. */
    default List<ChunkSearchResult> searchSparse(String query, int limit, double minimumScore) {
        throw new UnsupportedOperationException("Sparse search is not supported");
    }
}
