package com.csnotes.rag.persistence;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.ArrayList;

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

    @Override
    public Map<String, float[]> findReusableEmbeddings(
            String modelName,
            Set<String> contentHashes
    ) {
        if (contentHashes.isEmpty()) return Map.of();

        String placeholders = String.join(",", java.util.Collections.nCopies(contentHashes.size(), "?"));
        List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(modelName);
        parameters.addAll(contentHashes);
        Map<String, float[]> reusable = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT content_hash, embedding::text AS embedding
                          FROM document_chunk
                         WHERE embedding_model = ?
                           AND content_hash IN (%s)
                        """.formatted(placeholders), (RowCallbackHandler) resultSet -> reusable.putIfAbsent(
                        resultSet.getString("content_hash"), parseVector(resultSet.getString("embedding"))
                ), parameters.toArray());
        return Map.copyOf(reusable);
    }

    @Override
    public Set<String> findIndexedDocumentIds() {
        return Set.copyOf(jdbcTemplate.queryForList(
                "SELECT DISTINCT document_id FROM document_chunk", String.class
        ));
    }

    @Override
    public Map<String, IndexedDocumentState> findIndexedDocumentStates() {
        Map<String, MutableIndexedDocument> documents = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT document_id, document_title, document_path, tags, embedding_model,
                       sequence, content_hash, section_path
                  FROM document_chunk
                 ORDER BY document_id, sequence
                """, (RowCallbackHandler) resultSet -> {
            String documentId = resultSet.getString("document_id");
            String documentTitle = resultSet.getString("document_title");
            String documentPath = resultSet.getString("document_path");
            List<String> tags = fromJson(resultSet.getString("tags"));
            String embeddingModel = resultSet.getString("embedding_model");
            MutableIndexedDocument document = documents.computeIfAbsent(documentId, ignored ->
                    new MutableIndexedDocument(
                            documentId, documentTitle, documentPath, tags, embeddingModel, new ArrayList<>()
                    ));
            document.chunks().add(new IndexedDocumentState.IndexedChunkState(
                    resultSet.getInt("sequence"),
                    resultSet.getString("content_hash"),
                    fromJson(resultSet.getString("section_path"))
            ));
        });
        return documents.values().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                MutableIndexedDocument::documentId,
                document -> new IndexedDocumentState(
                        document.documentId(), document.documentTitle(), document.documentPath(),
                        document.tags(), document.embeddingModel(), List.copyOf(document.chunks())
                )
        ));
    }

    /** cosine distance를 cosine 유사도(-1~1)로 바꿔 높은 순서대로 반환한다. */
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

    /** 제목과 태그에 높은 가중치를 둔 PostgreSQL FTS 결과를 0~1 점수로 정규화한다. */
    @Override
    public List<ChunkSearchResult> searchSparse(String query, int limit, double minimumScore) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Search query must not be blank");
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Search limit must be between 1 and 100");
        return jdbcTemplate.query("""
                WITH query_value AS (
                    SELECT websearch_to_tsquery('simple', ?) AS value
                ), ranked AS (
                    SELECT chunk.*, ts_rank_cd(chunk.search_vector, query_value.value) AS raw_score
                      FROM document_chunk chunk
                      CROSS JOIN query_value
                     WHERE chunk.search_vector @@ query_value.value
                )
                SELECT id, document_id, document_title, document_path, tags, section_path,
                       sequence, content, content_hash, raw_score / (raw_score + 1.0) AS score
                  FROM ranked
                 WHERE raw_score / (raw_score + 1.0) >= ?
                 ORDER BY raw_score DESC, document_id, sequence
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
                }, query.strip(), minimumScore, limit);
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

    private float[] parseVector(String vector) {
        String value = vector.substring(1, vector.length() - 1);
        if (value.isBlank()) return new float[0];
        String[] parts = value.split(",");
        float[] result = new float[parts.length];
        for (int index = 0; index < parts.length; index++) {
            result[index] = Float.parseFloat(parts[index]);
        }
        return result;
    }

    private void validateDimensions(EmbeddingVector vector) {
        if (vector.dimensions() != dimensions) {
            throw new IllegalArgumentException("Embedding dimensions must match pgvector schema: " + dimensions);
        }
    }

    private record MutableIndexedDocument(
            String documentId,
            String documentTitle,
            String documentPath,
            List<String> tags,
            String embeddingModel,
            List<IndexedDocumentState.IndexedChunkState> chunks
    ) {
    }
}
