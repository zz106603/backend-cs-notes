package com.csnotes.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Validated
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
            @Valid @RequestBody DocumentModels.UpdateCategoryRequest request
    ) {
        return documentService.updateCategory(request);
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
            @Valid @RequestBody DocumentModels.CreateDocumentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(request));
    }

    @PutMapping("/documents/{id}")
    public DocumentModels.DocumentDetailResponse updateDocument(
            @PathVariable @NotBlank String id,
            @Valid @RequestBody DocumentModels.UpdateDocumentRequest request
    ) {
        return documentService.updateDocument(id, request);
    }

    @PostMapping("/documents/{id}/move")
    public DocumentModels.DocumentDetailResponse moveDocument(
            @PathVariable @NotBlank String id,
            @Valid @RequestBody DocumentModels.MoveDocumentRequest request
    ) {
        return documentService.moveDocument(id, request);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> moveDocumentToTrash(@PathVariable @NotBlank String id) {
        documentService.moveDocumentToTrash(id);
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
    public DocumentModels.DocumentDetailResponse restoreTrashDocument(@PathVariable @NotBlank String id) {
        return documentService.restoreTrashDocument(id);
    }
}
