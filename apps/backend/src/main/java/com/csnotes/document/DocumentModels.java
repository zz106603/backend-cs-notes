package com.csnotes.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class DocumentModels {

    private DocumentModels() {
    }

    public record CategoryResponse(
            String name,
            String path,
            long documentCount,
            List<CategoryResponse> children
    ) {
    }

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String path
    ) {
    }

    public record UpdateCategoryRequest(
            @NotBlank @Size(max = 100) String path,
            @NotBlank @Size(max = 100) String newPath
    ) {
    }

    public record DocumentSummaryResponse(
            String id,
            String title,
            String category,
            String path,
            Instant updatedAt,
            List<String> tags,
            String excerpt
    ) {
        public DocumentSummaryResponse(String id, String title, String category, String path, Instant updatedAt) {
            this(id, title, category, path, updatedAt, List.of(), null);
        }
    }

    public record DocumentDetailResponse(
            String id,
            String title,
            String category,
            String path,
            String content,
            Instant updatedAt,
            List<String> tags
    ) {
        public DocumentDetailResponse(
                String id, String title, String category, String path, String content, Instant updatedAt
        ) {
            this(id, title, category, path, content, updatedAt, List.of());
        }
    }

    public record CreateDocumentRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 100) String category,
            @NotNull @Size(max = 1_000_000) String content,
            @Size(max = 10) List<@NotBlank @Size(max = 30) String> tags
    ) {
        public CreateDocumentRequest(String title, String category, String content) {
            this(title, category, content, List.of());
        }
    }

    public record UpdateDocumentRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 100) String category,
            @NotNull @Size(max = 1_000_000) String content,
            @Size(max = 10) List<@NotBlank @Size(max = 30) String> tags,
            @NotNull Instant expectedUpdatedAt
    ) {
        public UpdateDocumentRequest(String title, String category, String content, Instant expectedUpdatedAt) {
            this(title, category, content, List.of(), expectedUpdatedAt);
        }
    }

    public record MoveDocumentRequest(
            @NotBlank @Size(max = 100) String category,
            @NotNull Instant expectedUpdatedAt
    ) {
    }

    public record TrashDocumentResponse(
            String id,
            String title,
            String originalPath,
            Instant deletedAt
    ) {
    }
}
