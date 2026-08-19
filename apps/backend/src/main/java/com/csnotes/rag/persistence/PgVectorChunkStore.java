package com.csnotes.rag.persistence;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Repository
@ConditionalOnProperty(name = "rag.persistence.enabled", havingValue = "true")
public class PgVectorChunkStore implements ChunkVectorStore {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int dimensions;

    public PgVectorChunkStore(
            JdbcTemplate ragJdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${rag.embedding.dimensions}") int dimensions
    ) {
        this.jdbcTemplate = ragJdbcTemplate;
        this.objectMapper = objectMapper;
        this.dimensions = dimensions;
    }

    /** 문서 하나의 기존 Chunk를 지우고 새 Chunk를 같은 트랜잭션에서 저장한다. */
    @Override
    @Transactional("ragTransactionManager")
    public void replaceDocumentChunks(String documentId, List<EmbeddedChunk> chunks) {
        deleteDocument(documentId);
        for (EmbeddedChunk embeddedChunk : chunks) {
            validateDimensions(embeddedChunk.embedding());
            DocumentChunk chunk = embeddedChunk.chunk();
            jdbcTemplate.update("""
                    INSERT INTO document_chunk (
                        id, document_id, document_title, document_path, tags, section_path,
                        sequence, content, content_hash, embedding_model, embedding
                    ) VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS vector))
                    """, chunk.id(), chunk.documentId(), chunk.documentTitle(), chunk.documentPath(),
                    toJson(chunk.tags()), toJson(chunk.sectionPath()), chunk.sequence(), chunk.content(),
                    chunk.contentHash(), embeddedChunk.embedding().model(), toVector(embeddedChunk.embedding().values()));
        }
    }

    @Override
    public void deleteDocument(String documentId) {
        jdbcTemplate.update("DELETE FROM document_chunk WHERE document_id = ?", documentId);
    }

    /** cosine distance를 0~1 유사도 점수로 바꿔 높은 순서대로 반환한다. */
    @Override
    public List<ChunkSearchResult> search(EmbeddingVector query, int limit, double minimumScore) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Search limit must be between 1 and 100");
        validateDimensions(query);
        String vector = toVector(query.values());
        return jdbcTemplate.query("""
                SELECT id, document_id, document_title, document_path, tags, section_path,
                       sequence, content, content_hash, 1 - (embedding <=> CAST(? AS vector)) AS score
                  FROM document_chunk
                 WHERE embedding_model = ?
                   AND 1 - (embedding <=> CAST(? AS vector)) >= ?
                 ORDER BY embedding <=> CAST(? AS vector)
                 LIMIT ?
                """, (resultSet, rowNumber) -> {
                    DocumentChunk chunk = new DocumentChunk(
                            resultSet.getString("id"), resultSet.getString("document_id"),
                            resultSet.getString("document_title"), resultSet.getString("document_path"),
                            fromJson(resultSet.getString("tags")), fromJson(resultSet.getString("section_path")),
                            resultSet.getInt("sequence"), resultSet.getString("content"),
                            resultSet.getString("content_hash")
                    );
                    return new ChunkSearchResult(chunk, resultSet.getDouble("score"));
                }, vector, query.model(), vector, minimumScore, vector, limit);
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Chunk metadata cannot be serialized", exception);
        }
    }

    private List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored chunk metadata cannot be read", exception);
        }
    }

    private String toVector(float[] values) {
        return IntStream.range(0, values.length)
                .mapToObj(index -> Float.toString(values[index]))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private void validateDimensions(EmbeddingVector vector) {
        if (vector.dimensions() != dimensions) {
            throw new IllegalArgumentException("Embedding dimensions must match pgvector schema: " + dimensions);
        }
    }
}
