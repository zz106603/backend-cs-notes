package com.csnotes.document.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = {"cs-notes.security.enabled", "rag.persistence.enabled"}, havingValue = "true")
public class DocumentMetadataRepository {
    private static final RowMapper<DocumentMetadata> ROW_MAPPER = (resultSet, rowNumber) -> new DocumentMetadata(
            resultSet.getObject("id", UUID.class),
            resultSet.getObject("owner_id", UUID.class),
            resultSet.getString("source_document_id"),
            resultSet.getString("file_path"),
            resultSet.getString("title"),
            DocumentVisibility.valueOf(resultSet.getString("visibility")),
            resultSet.getString("content_hash"),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant(),
            nullableInstant(resultSet.getTimestamp("deleted_at"))
    );
    private final JdbcTemplate jdbcTemplate;

    public DocumentMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, DocumentMetadata> findByOwnerId(UUID ownerId) {
        return jdbcTemplate.query("""
                        SELECT id, owner_id, source_document_id, file_path, title, visibility,
                               content_hash, created_at, updated_at, deleted_at
                          FROM document
                         WHERE owner_id = ?
                        """, ROW_MAPPER, ownerId).stream()
                .collect(Collectors.toMap(DocumentMetadata::filePath, Function.identity()));
    }

    public Optional<DocumentMetadata> findByOwnerIdAndPath(UUID ownerId, String filePath) {
        return jdbcTemplate.query("""
                        SELECT id, owner_id, source_document_id, file_path, title, visibility,
                               content_hash, created_at, updated_at, deleted_at
                          FROM document
                         WHERE owner_id = ? AND file_path = ?
                        """, ROW_MAPPER, ownerId, filePath).stream().findFirst();
    }

    public Map<String, DocumentMetadata> findActiveByOwnerIdIndexedBySourceId(UUID ownerId) {
        return findByOwnerId(ownerId).values().stream()
                .filter(metadata -> metadata.deletedAt() == null)
                .collect(Collectors.toMap(DocumentMetadata::sourceDocumentId, Function.identity()));
    }

    /** file_path의 유일성으로 다른 사용자가 이미 소유한 파일을 덮어쓰지 못하게 한다. */
    public boolean insert(UUID ownerId, MetadataSource source) {
        return jdbcTemplate.update("""
                INSERT INTO document (
                    id, owner_id, source_document_id, file_path, title, visibility, content_hash
                ) VALUES (?, ?, ?, ?, ?, 'PRIVATE', ?)
                ON CONFLICT (file_path) DO NOTHING
                """, UUID.randomUUID(), ownerId, source.sourceDocumentId(), source.filePath(),
                source.title(), source.contentHash()) == 1;
    }

    public void update(UUID documentId, MetadataSource source) {
        jdbcTemplate.update("""
                UPDATE document
                   SET source_document_id = ?, file_path = ?, title = ?, content_hash = ?,
                       updated_at = CURRENT_TIMESTAMP, deleted_at = NULL
                 WHERE id = ?
                """, source.sourceDocumentId(), source.filePath(), source.title(), source.contentHash(), documentId);
    }

    public boolean updateByOwnerAndPath(UUID ownerId, String previousPath, MetadataSource source) {
        return jdbcTemplate.update("""
                UPDATE document
                   SET source_document_id = ?, file_path = ?, title = ?, content_hash = ?,
                       updated_at = CURRENT_TIMESTAMP, deleted_at = NULL
                 WHERE owner_id = ? AND file_path = ?
                """, source.sourceDocumentId(), source.filePath(), source.title(), source.contentHash(),
                ownerId, previousPath) == 1;
    }

    public void markDeletedByOwnerAndPath(UUID ownerId, String filePath, Instant deletedAt) {
        jdbcTemplate.update("""
                UPDATE document
                   SET deleted_at = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE owner_id = ? AND file_path = ? AND deleted_at IS NULL
                """, Timestamp.from(deletedAt), ownerId, filePath);
    }

    public void markDeleted(List<UUID> documentIds, Instant deletedAt) {
        documentIds.forEach(documentId -> jdbcTemplate.update("""
                UPDATE document
                   SET deleted_at = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE id = ? AND deleted_at IS NULL
                """, Timestamp.from(deletedAt), documentId));
    }

    private static Instant nullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record MetadataSource(String sourceDocumentId, String filePath, String title, String contentHash) {
    }
}
