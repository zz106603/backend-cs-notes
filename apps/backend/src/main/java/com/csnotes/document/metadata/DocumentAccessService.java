package com.csnotes.document.metadata;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentNotFoundException;
import com.csnotes.document.DocumentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = {"cs-notes.security.enabled", "rag.persistence.enabled"}, havingValue = "true")
public class DocumentAccessService {
    private final DocumentService documentService;
    private final DocumentMetadataRepository repository;

    public DocumentAccessService(DocumentService documentService, DocumentMetadataRepository repository) {
        this.documentService = documentService;
        this.repository = repository;
    }

    public List<DocumentModels.DocumentSummaryResponse> findReadableDocuments(
            UUID userId, String category, String query
    ) {
        Set<String> readableIds = repository.findReadableSourceDocumentIds(userId);
        return documentService.findDocuments(category, query).stream()
                .filter(document -> readableIds.contains(document.id()))
                .toList();
    }

    /** 폴더 개수도 전체 파일 수가 아닌 현재 사용자가 읽을 수 있는 문서만으로 다시 계산한다. */
    public List<DocumentModels.CategoryResponse> findReadableCategories(UUID userId) {
        List<DocumentModels.DocumentSummaryResponse> readable = findReadableDocuments(userId, null, null);
        return recalculateCounts(documentService.findCategories(), readable);
    }

    public List<DocumentModels.TrashDocumentResponse> findOwnedTrashDocuments(UUID userId) {
        Set<String> ownedDeletedIds = repository.findOwnedSourceDocumentIds(userId, true);
        return documentService.findTrashDocuments().stream()
                .filter(document -> ownedDeletedIds.contains(document.id()))
                .toList();
    }

    public void requireReadable(UUID userId, String documentId) {
        if (!repository.canRead(userId, documentId)) notFound();
    }

    /** 파일에서 읽은 본문에 DB의 공개 범위와 현재 사용자의 소유 여부를 결합한다. */
    public DocumentModels.DocumentDetailResponse attachAccess(
            UUID userId, DocumentModels.DocumentDetailResponse document
    ) {
        DocumentMetadata metadata = repository.findReadableBySourceDocumentId(userId, document.id())
                .orElseThrow(() -> new DocumentNotFoundException("문서를 찾을 수 없습니다."));
        return new DocumentModels.DocumentDetailResponse(
                document.id(), document.title(), document.category(), document.path(), document.content(),
                document.updatedAt(), document.tags(), metadata.visibility(), metadata.ownerId().equals(userId));
    }

    public DocumentModels.DocumentDetailResponse updateVisibility(
            UUID userId, DocumentModels.DocumentDetailResponse document, DocumentVisibility visibility
    ) {
        repository.updateVisibility(userId, document.id(), visibility);
        return new DocumentModels.DocumentDetailResponse(
                document.id(), document.title(), document.category(), document.path(), document.content(),
                document.updatedAt(), document.tags(), visibility, true);
    }

    public void requireOwner(UUID userId, String documentId) {
        if (!repository.isOwner(userId, documentId, false)) notFound();
    }

    public void requireDeletedOwner(UUID userId, String documentId) {
        if (!repository.isOwner(userId, documentId, true)) notFound();
    }

    /** 폴더 이동은 하위 파일 전체를 변경하므로 하나라도 본인 소유가 아니면 실행하지 않는다. */
    public void requireCategoryOwner(UUID userId, String category) {
        List<DocumentModels.DocumentSummaryResponse> affected = documentService.findDocuments(category, null);
        Set<String> ownedIds = repository.findOwnedSourceDocumentIds(userId, false);
        if (affected.stream().anyMatch(document -> !ownedIds.contains(document.id()))) notFound();
    }

    private List<DocumentModels.CategoryResponse> recalculateCounts(
            List<DocumentModels.CategoryResponse> categories,
            List<DocumentModels.DocumentSummaryResponse> readable
    ) {
        return categories.stream().map(category -> new DocumentModels.CategoryResponse(
                category.name(), category.path(),
                readable.stream().filter(document -> document.category().equals(category.path())
                        || document.category().startsWith(category.path() + "/")).count(),
                recalculateCounts(category.children(), readable)
        )).toList();
    }

    private void notFound() {
        throw new DocumentNotFoundException("문서를 찾을 수 없습니다.");
    }
}
