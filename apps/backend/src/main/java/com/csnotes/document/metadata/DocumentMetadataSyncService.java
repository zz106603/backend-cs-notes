package com.csnotes.document.metadata;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = {"cs-notes.security.enabled", "rag.persistence.enabled"}, havingValue = "true")
public class DocumentMetadataSyncService {
    private final DocumentService documentService;
    private final DocumentMetadataRepository repository;

    public DocumentMetadataSyncService(DocumentService documentService, DocumentMetadataRepository repository) {
        this.documentService = documentService;
        this.repository = repository;
    }

    /** 파일 시스템을 원본으로 삼아 로그인 사용자의 문서 메타데이터를 한 트랜잭션에서 맞춘다. */
    @Transactional("ragTransactionManager")
    public DocumentMetadataSyncResult synchronize(UUID ownerId) {
        List<DocumentModels.DocumentSummaryResponse> documents = documentService.findDocuments(null, null);
        Map<String, DocumentMetadata> storedByPath = repository.findByOwnerId(ownerId);
        Set<String> currentPaths = new HashSet<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int ownershipConflicts = 0;

        for (DocumentModels.DocumentSummaryResponse summary : documents) {
            DocumentModels.DocumentDetailResponse detail = documentService.findDocument(summary.id())
                    .orElseThrow(() -> new IllegalStateException("동기화 중 문서가 사라졌습니다: " + summary.path()));
            currentPaths.add(summary.path());
            DocumentMetadataRepository.MetadataSource source = new DocumentMetadataRepository.MetadataSource(
                    summary.id(), summary.path(), summary.title(), DocumentContentHasher.sha256(detail.content()));
            DocumentMetadata stored = storedByPath.get(summary.path());
            if (stored == null) {
                if (repository.insert(ownerId, source)) created++;
                else ownershipConflicts++;
            } else if (requiresUpdate(stored, source)) {
                repository.update(stored.id(), source);
                updated++;
            } else {
                unchanged++;
            }
        }

        List<UUID> deletedIds = new ArrayList<>();
        storedByPath.values().stream()
                .filter(metadata -> metadata.deletedAt() == null)
                .filter(metadata -> !currentPaths.contains(metadata.filePath()))
                .map(DocumentMetadata::id)
                .forEach(deletedIds::add);
        repository.markDeleted(deletedIds, Instant.now());
        return new DocumentMetadataSyncResult(
                documents.size(), created, updated, unchanged, deletedIds.size(), ownershipConflicts);
    }

    private boolean requiresUpdate(
            DocumentMetadata stored,
            DocumentMetadataRepository.MetadataSource source
    ) {
        return stored.deletedAt() != null
                || !stored.sourceDocumentId().equals(source.sourceDocumentId())
                || !stored.title().equals(source.title())
                || !stored.contentHash().equals(source.contentHash());
    }

}
