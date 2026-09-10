package com.csnotes.document;

import com.csnotes.auth.CsNotesOidcUser;
import com.csnotes.document.metadata.DocumentMetadataLifecycleService;
import com.csnotes.document.metadata.DocumentMetadataSyncService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final ObjectProvider<DocumentMetadataLifecycleService> metadataLifecycleService;
    private final ObjectProvider<DocumentMetadataSyncService> metadataSyncService;

    public DocumentController(
            DocumentService documentService,
            ObjectProvider<DocumentMetadataLifecycleService> metadataLifecycleService,
            ObjectProvider<DocumentMetadataSyncService> metadataSyncService
    ) {
        this.documentService = documentService;
        this.metadataLifecycleService = metadataLifecycleService;
        this.metadataSyncService = metadataSyncService;
    }

    @GetMapping("/categories")
    public List<DocumentModels.CategoryResponse> categories() {
        return documentService.findCategories();
    }

    @PostMapping("/categories")
    public ResponseEntity<DocumentModels.CategoryResponse> createCategory(
            @Valid @RequestBody DocumentModels.CreateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createCategory(request));
    }

    @PutMapping("/categories")
    public DocumentModels.CategoryResponse updateCategory(
            @Valid @RequestBody DocumentModels.UpdateCategoryRequest request,
            Authentication authentication
    ) {
        DocumentModels.CategoryResponse response = documentService.updateCategory(request);
        currentUserId(authentication).ifPresent(userId ->
                metadataSyncService.ifAvailable(service -> service.synchronize(userId)));
        return response;
    }

    @GetMapping("/documents")
    public List<DocumentModels.DocumentSummaryResponse> documents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query
    ) {
        return documentService.findDocuments(category, query);
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentModels.DocumentDetailResponse> document(
            @PathVariable @NotBlank String id
    ) {
        return documentService.findDocument(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/documents")
    public ResponseEntity<DocumentModels.DocumentDetailResponse> createDocument(
            @Valid @RequestBody DocumentModels.CreateDocumentRequest request,
            Authentication authentication
    ) {
        DocumentModels.DocumentDetailResponse response = documentService.createDocument(request);
        currentUserId(authentication).ifPresent(userId ->
                metadataLifecycleService.ifAvailable(service -> service.registerOrRestore(userId, response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/documents/{id}")
    public DocumentModels.DocumentDetailResponse updateDocument(
            @PathVariable @NotBlank String id,
            @Valid @RequestBody DocumentModels.UpdateDocumentRequest request,
            Authentication authentication
    ) {
        Optional<UUID> userId = currentUserId(authentication);
        String previousPath = previousPathIfRequired(userId, id);
        DocumentModels.DocumentDetailResponse response = documentService.updateDocument(id, request);
        userId.ifPresent(ownerId -> metadataLifecycleService.ifAvailable(
                service -> service.update(ownerId, previousPath, response)));
        return response;
    }

    @PostMapping("/documents/{id}/move")
    public DocumentModels.DocumentDetailResponse moveDocument(
            @PathVariable @NotBlank String id,
            @Valid @RequestBody DocumentModels.MoveDocumentRequest request,
            Authentication authentication
    ) {
        Optional<UUID> userId = currentUserId(authentication);
        String previousPath = previousPathIfRequired(userId, id);
        DocumentModels.DocumentDetailResponse response = documentService.moveDocument(id, request);
        userId.ifPresent(ownerId -> metadataLifecycleService.ifAvailable(
                service -> service.update(ownerId, previousPath, response)));
        return response;
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> moveDocumentToTrash(
            @PathVariable @NotBlank String id,
            Authentication authentication
    ) {
        Optional<UUID> userId = currentUserId(authentication);
        String previousPath = previousPathIfRequired(userId, id);
        documentService.moveDocumentToTrash(id);
        userId.ifPresent(ownerId -> metadataLifecycleService.ifAvailable(
                service -> service.markDeleted(ownerId, previousPath)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    public List<DocumentModels.TrashDocumentResponse> trashDocuments() {
        return documentService.findTrashDocuments();
    }

    @DeleteMapping("/trash/{id}")
    public ResponseEntity<Void> permanentlyDeleteTrashDocument(@PathVariable @NotBlank String id) {
        documentService.permanentlyDeleteTrashDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trash/{id}/restore")
    public DocumentModels.DocumentDetailResponse restoreTrashDocument(
            @PathVariable @NotBlank String id,
            Authentication authentication
    ) {
        DocumentModels.DocumentDetailResponse response = documentService.restoreTrashDocument(id);
        currentUserId(authentication).ifPresent(userId ->
                metadataLifecycleService.ifAvailable(service -> service.registerOrRestore(userId, response)));
        return response;
    }

    private Optional<UUID> currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CsNotesOidcUser principal) {
            return Optional.of(principal.userId());
        }
        return Optional.empty();
    }

    /** 인증 모드에서만 변경 전 경로를 읽어 문서 UUID가 이동 뒤에도 유지되게 한다. */
    private String previousPathIfRequired(Optional<UUID> userId, String documentId) {
        if (userId.isEmpty() || metadataLifecycleService.getIfAvailable() == null) return null;
        return documentService.findDocument(documentId)
                .map(DocumentModels.DocumentDetailResponse::path)
                .orElse(null);
    }
}
