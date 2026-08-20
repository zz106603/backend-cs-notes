package com.csnotes.rag.answer;

public record RagAnswerRequest(String question, Integer sourceLimit, Double minimumScore) {
}
