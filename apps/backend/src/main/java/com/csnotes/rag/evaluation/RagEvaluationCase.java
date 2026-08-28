package com.csnotes.rag.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RagEvaluationCase(
        UUID id,
        String query,
        List<String> expectedDocumentPaths,
        Instant createdAt
) {
}
