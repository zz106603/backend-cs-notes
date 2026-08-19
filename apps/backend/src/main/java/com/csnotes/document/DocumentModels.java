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

    public record DocumentSummaryResponse(
            String id,
            String title,
            String category,
            String path,
            Instant updatedAt
    ) {
    }

    public record DocumentDetailResponse(
            String id,
            String title,
            String category,
            String path,
            String content,
            Instant updatedAt
    ) {
    }

    public record CreateDocumentRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 100) String category,
            @NotNull @Size(max = 1_000_000) String content
    ) {
    }

    public record UpdateDocumentRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 100) String category,
            @NotNull @Size(max = 1_000_000) String content,
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
