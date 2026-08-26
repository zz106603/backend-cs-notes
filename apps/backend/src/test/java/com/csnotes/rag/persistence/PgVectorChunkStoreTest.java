package com.csnotes.rag.persistence;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingVector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgVectorChunkStoreTest {
    @Test
    void 문서의_기존_청크를_삭제한_뒤_새_청크를_저장한다() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        var store = new PgVectorChunkStore(jdbcTemplate, new ObjectMapper(), 2);
        var chunk = new DocumentChunk("chunk-1", "doc-1", "제목", "경로.md", List.of("RAG"),
                List.of("검색"), 0, "본문", "a".repeat(64));
        var embedded = new EmbeddedChunk(chunk, new EmbeddingVector("chunk-1", "test-model", new float[]{1, 0}));

        store.replaceDocumentChunks("doc-1", List.of(embedded));

        verify(jdbcTemplate).update("DELETE FROM document_chunk WHERE document_id = ?", "doc-1");
        verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    void 검색_결과_개수의_허용_범위를_검증한다() {
        var store = new PgVectorChunkStore(mock(JdbcTemplate.class), new ObjectMapper(), 2);
        var query = new EmbeddingVector("query", "test-model", new float[]{1, 0});

        assertThatThrownBy(() -> store.search(query, 0, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.search(query, 101, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 희소_검색의_검색어와_결과_개수를_검증한다() {
        var store = new PgVectorChunkStore(mock(JdbcTemplate.class), new ObjectMapper(), 2);

        assertThatThrownBy(() -> store.searchSparse(" ", 5, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.searchSparse("REQUIRES_NEW", 0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.searchSparse("REQUIRES_NEW", 101, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 스키마와_다른_차원의_벡터를_거부한다() {
        var store = new PgVectorChunkStore(mock(JdbcTemplate.class), new ObjectMapper(), 3);
        var query = new EmbeddingVector("query", "test-model", new float[]{1, 0});

        assertThatThrownBy(() -> store.search(query, 5, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions");
    }

    @Test
    void 같은_본문_해시와_모델의_저장된_벡터를_읽는다() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("content_hash")).thenReturn("hash-1");
        when(resultSet.getString("embedding")).thenReturn("[0.1,0.2]");
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(resultSet);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
        var store = new PgVectorChunkStore(jdbcTemplate, new ObjectMapper(), 2);

        var reusable = store.findReusableEmbeddings("test-model", Set.of("hash-1"));

        assertThat(reusable.get("hash-1")).containsExactly(0.1f, 0.2f);
    }
}
