package com.csnotes.rag.evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RagEvaluationRepository {
    List<RagEvaluationCase> findAll();
    Optional<RagEvaluationCase> findById(UUID id);
    void save(RagEvaluationCase evaluationCase);
    void deleteById(UUID id);
}
