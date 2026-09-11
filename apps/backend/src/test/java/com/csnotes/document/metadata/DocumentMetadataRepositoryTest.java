package com.csnotes.document.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DocumentMetadataRepositoryTest {
    @Test
    void 삭제_시각을_PostgreSQL이_인식할_Timestamp로_전달한다() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DocumentMetadataRepository repository = new DocumentMetadataRepository(jdbcTemplate);
        UUID ownerId = UUID.randomUUID();
        Instant deletedAt = Instant.parse("2026-09-10T01:00:00Z");

        repository.markDeletedByOwnerAndPath(ownerId, "백엔드/Spring.md", deletedAt);

        verify(jdbcTemplate).update(
                anyString(), eq(Timestamp.from(deletedAt)), eq(ownerId), eq("백엔드/Spring.md"));
    }

    @Test
    void 전체_동기화의_삭제_처리도_Timestamp를_사용한다() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DocumentMetadataRepository repository = new DocumentMetadataRepository(jdbcTemplate);
        UUID documentId = UUID.randomUUID();
        Instant deletedAt = Instant.parse("2026-09-10T01:00:00Z");

        repository.markDeleted(List.of(documentId), deletedAt);

        verify(jdbcTemplate).update(anyString(), eq(Timestamp.from(deletedAt)), eq(documentId));
    }
}
