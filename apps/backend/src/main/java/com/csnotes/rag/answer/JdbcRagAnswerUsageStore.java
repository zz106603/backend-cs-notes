package com.csnotes.rag.answer;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

/** RAG 호출별 토큰과 예상 비용을 저장해 비용 한도의 근거로 사용한다. */
final class JdbcRagAnswerUsageStore implements RagAnswerUsageStore {
    private final JdbcTemplate jdbcTemplate;

    JdbcRagAnswerUsageStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BigDecimal totalCostBetween(Instant fromInclusive, Instant toExclusive) {
        BigDecimal total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(estimated_cost_usd), 0)
                FROM rag_answer_usage
                WHERE created_at >= ? AND created_at < ?
                """, BigDecimal.class, Timestamp.from(fromInclusive), Timestamp.from(toExclusive));
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public void save(RagAnswerUsageRecord record) {
        jdbcTemplate.update("""
                INSERT INTO rag_answer_usage (
                    request_id, question_hash, model, status,
                    prompt_tokens, completion_tokens, total_tokens,
                    estimated_cost_usd, source_count, context_characters,
                    elapsed_ms, failure_type, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                record.requestId(), record.questionHash(), record.model(), record.status(),
                record.usage().promptTokens(), record.usage().completionTokens(), record.usage().totalTokens(),
                record.estimatedCostUsd(), record.sourceCount(), record.contextCharacters(),
                record.elapsedMs(), record.failureType(), Timestamp.from(record.createdAt()));
    }
}

