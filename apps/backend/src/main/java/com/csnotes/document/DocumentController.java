package com.csnotes.document;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
