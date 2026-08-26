package com.csnotes.rag.search;

public record RagSearchRequest(
        String query,
        Integer limit,
        Double minimumScore,
        RagSearchMode mode
) {
}
