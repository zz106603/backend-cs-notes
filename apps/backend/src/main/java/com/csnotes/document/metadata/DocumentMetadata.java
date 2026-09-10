package com.csnotes.document.metadata;

import java.time.Instant;
import java.util.UUID;

public record DocumentMetadata(
        UUID id,
        UUID ownerId,
        String sourceDocumentId,
        String filePath,
        String title,
        DocumentVisibility visibility,
        String contentHash,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
