package com.csnotes.document.metadata;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataSyncServiceTest {
    @Mock
    DocumentService documentService;

    @Mock
    DocumentMetadataRepository repository;

    @Captor
    ArgumentCaptor<List<UUID>> deletedIds;

    @Test
    void 새_문서를_등록하고_사라진_문서를_삭제_표시한다() {
        UUID ownerId = UUID.randomUUID();
        UUID missingDocumentId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-09-10T00:00:00Z");
        DocumentModels.DocumentSummaryResponse summary = new DocumentModels.DocumentSummaryResponse(
                "source-a", "트랜잭션", "데이터베이스", "데이터베이스/트랜잭션.md", updatedAt);
        DocumentModels.DocumentDetailResponse detail = new DocumentModels.DocumentDetailResponse(
                "source-a", "트랜잭션", "데이터베이스", "데이터베이스/트랜잭션.md", "본문", updatedAt);
        DocumentMetadata missing = new DocumentMetadata(
                missingDocumentId, ownerId, "source-old", "운영체제/삭제됨.md", "삭제됨",
                DocumentVisibility.PRIVATE, "old-hash", updatedAt, updatedAt, null);
        when(documentService.findDocuments(null, null)).thenReturn(List.of(summary));
        when(documentService.findDocument("source-a")).thenReturn(Optional.of(detail));
        when(repository.findByOwnerId(ownerId)).thenReturn(Map.of(missing.filePath(), missing));
        when(repository.insert(any(), any())).thenReturn(true);
        DocumentMetadataSyncService service = new DocumentMetadataSyncService(documentService, repository);

        DocumentMetadataSyncResult result = service.synchronize(ownerId);

        assertThat(result).isEqualTo(new DocumentMetadataSyncResult(1, 1, 0, 0, 1, 0));
        verify(repository).markDeleted(deletedIds.capture(), any(Instant.class));
        assertThat(deletedIds.getValue()).containsExactly(missingDocumentId);
    }

    @Test
    void 다른_사용자가_소유한_경로는_충돌로_집계한다() {
        UUID ownerId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-09-10T00:00:00Z");
        DocumentModels.DocumentSummaryResponse summary = new DocumentModels.DocumentSummaryResponse(
                "source-a", "인덱스", "데이터베이스", "데이터베이스/인덱스.md", updatedAt);
        DocumentModels.DocumentDetailResponse detail = new DocumentModels.DocumentDetailResponse(
                "source-a", "인덱스", "데이터베이스", "데이터베이스/인덱스.md", "본문", updatedAt);
        when(documentService.findDocuments(null, null)).thenReturn(List.of(summary));
        when(documentService.findDocument("source-a")).thenReturn(Optional.of(detail));
        when(repository.findByOwnerId(ownerId)).thenReturn(Map.of());
        when(repository.insert(any(), any())).thenReturn(false);
        DocumentMetadataSyncService service = new DocumentMetadataSyncService(documentService, repository);

        DocumentMetadataSyncResult result = service.synchronize(ownerId);

        assertThat(result.ownershipConflicts()).isEqualTo(1);
        assertThat(result.created()).isZero();
    }
}
