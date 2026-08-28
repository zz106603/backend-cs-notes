package com.csnotes.rag.evaluation;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class JdbcRagEvaluationRepository implements RagEvaluationRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcRagEvaluationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RagEvaluationCase> findAll() {
        return jdbcTemplate.query("""
                SELECT id, query, expected_document_paths, created_at
                FROM rag_search_evaluation_case
                ORDER BY created_at DESC
                """, this::mapCase);
    }

    @Override
    public Optional<RagEvaluationCase> findById(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, query, expected_document_paths, created_at
                FROM rag_search_evaluation_case
                WHERE id = ?
                """, this::mapCase, id).stream().findFirst();
    }

    @Override
    public void save(RagEvaluationCase evaluationCase) {
        jdbcTemplate.update("""
                INSERT INTO rag_search_evaluation_case (id, query, expected_document_paths, created_at)
                VALUES (?, ?, ?, ?)
                """, evaluationCase.id(), evaluationCase.query(),
                String.join("\n", evaluationCase.expectedDocumentPaths()), Timestamp.from(evaluationCase.createdAt()));
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM rag_search_evaluation_case WHERE id = ?", id);
    }

    private RagEvaluationCase mapCase(ResultSet resultSet, int rowNumber) throws SQLException {
        String paths = resultSet.getString("expected_document_paths");
        return new RagEvaluationCase(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("query"),
                paths.isBlank() ? List.of() : Arrays.asList(paths.split("\\n")),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
