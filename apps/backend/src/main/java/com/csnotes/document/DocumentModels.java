package com.csnotes.document;

import java.time.Instant;

public final class DocumentModels {

    private DocumentModels() {
    }

    public record CategoryResponse(String name, long documentCount) {
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
}
