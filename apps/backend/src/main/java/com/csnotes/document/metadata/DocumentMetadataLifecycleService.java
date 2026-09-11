package com.csnotes.document.metadata;

import com.csnotes.document.DocumentModels;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = {"cs-notes.security.enabled", "rag.persistence.enabled"}, havingValue = "true")
public class DocumentMetadataLifecycleService {
    private final DocumentMetadataRepository repository;

    public DocumentMetadataLifecycleService(DocumentMetadataRepository repository) {
        this.repository = repository;
    }

    /** 생성·복원 시 현재 파일 정보로 등록하며, 이미 자신의 문서이면 삭제 표시도 함께 해제한다. */
    @Transactional("ragTransactionManager")
    public void registerOrRestore(UUID ownerId, DocumentModels.DocumentDetailResponse document) {
        DocumentMetadataRepository.MetadataSource source = sourceOf(document);
        repository.findByOwnerIdAndPath(ownerId, document.path())
                .ifPresentOrElse(
                        metadata -> repository.update(metadata.id(), source),
                        () -> {
                            if (!repository.insert(ownerId, source)) {
                                throw new DocumentOwnershipConflictException(document.path());
                            }
                        });
    }

    /** 수정·이동 전 경로로 기존 UUID를 찾아 새 경로에 그대로 연결한다. */
    @Transactional("ragTransactionManager")
    public void update(UUID ownerId, String previousPath, DocumentModels.DocumentDetailResponse document) {
        DocumentMetadataRepository.MetadataSource source = sourceOf(document);
        try {
            if (!repository.updateByOwnerAndPath(ownerId, previousPath, source)
                    && !repository.insert(ownerId, source)) {
                throw new DocumentOwnershipConflictException(document.path());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new DocumentOwnershipConflictException(document.path());
        }
    }

    @Transactional("ragTransactionManager")
    public void markDeleted(UUID ownerId, String filePath) {
        repository.markDeletedByOwnerAndPath(ownerId, filePath, Instant.now());
    }

    private DocumentMetadataRepository.MetadataSource sourceOf(DocumentModels.DocumentDetailResponse document) {
        return new DocumentMetadataRepository.MetadataSource(
                document.id(), document.path(), document.title(), DocumentContentHasher.sha256(document.content()));
    }
}
