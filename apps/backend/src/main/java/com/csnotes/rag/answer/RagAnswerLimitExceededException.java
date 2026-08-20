package com.csnotes.rag.answer;

public class RagAnswerLimitExceededException extends RuntimeException {
    RagAnswerLimitExceededException(String message) {
        super(message);
    }
}

