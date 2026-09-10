package com.csnotes.document.metadata;

import com.csnotes.document.DocumentModels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataLifecycleServiceTest {
    @Mock
    DocumentMetadataRepository repository;

    @Test
    void 새_문서를_현재_사용자_소유로_등록한다() {
        UUID ownerId = UUID.randomUUID();
        DocumentModels.DocumentDetailResponse document = document("id-new", "데이터베이스/인덱스.md", "본문");
        when(repository.findByOwnerIdAndPath(ownerId, document.path())).thenReturn(Optional.empty());
        when(repository.insert(eq(ownerId), any())).thenReturn(true);
        DocumentMetadataLifecycleService service = new DocumentMetadataLifecycleService(repository);

        service.registerOrRestore(ownerId, document);

        verify(repository).insert(eq(ownerId), any(DocumentMetadataRepository.MetadataSource.class));
    }

    @Test
    void 이동한_문서는_기존_경로를_기준으로_UUID를_유지한다() {
        UUID ownerId = UUID.randomUUID();
        DocumentModels.DocumentDetailResponse moved = document("id-moved", "자료구조/트리/인덱스.md", "본문");
        when(repository.updateByOwnerAndPath(eq(ownerId), eq("자료구조/인덱스.md"), any())).thenReturn(true);
        DocumentMetadataLifecycleService service = new DocumentMetadataLifecycleService(repository);

        service.update(ownerId, "자료구조/인덱스.md", moved);

        verify(repository).updateByOwnerAndPath(
                eq(ownerId), eq("자료구조/인덱스.md"), any(DocumentMetadataRepository.MetadataSource.class));
    }

    @Test
    void 휴지통으로_이동하면_메타데이터를_논리_삭제한다() {
        UUID ownerId = UUID.randomUUID();
        DocumentMetadataLifecycleService service = new DocumentMetadataLifecycleService(repository);

        service.markDeleted(ownerId, "운영체제/프로세스.md");

        verify(repository).markDeletedByOwnerAndPath(
                eq(ownerId), eq("운영체제/프로세스.md"), any(Instant.class));
    }

    private DocumentModels.DocumentDetailResponse document(String id, String path, String content) {
        return new DocumentModels.DocumentDetailResponse(
                id, "인덱스", path.substring(0, path.lastIndexOf('/')), path, content, Instant.now());
    }
}
