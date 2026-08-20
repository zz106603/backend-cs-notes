package com.csnotes.rag.answer;

public record RagAnswerUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    public static RagAnswerUsage unknown() {
        return new RagAnswerUsage(null, null, null);
    }
}
