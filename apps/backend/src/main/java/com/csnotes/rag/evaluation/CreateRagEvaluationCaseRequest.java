package com.csnotes.rag.evaluation;

import java.util.List;

public record CreateRagEvaluationCaseRequest(String query, List<String> expectedDocumentPaths) {
}
