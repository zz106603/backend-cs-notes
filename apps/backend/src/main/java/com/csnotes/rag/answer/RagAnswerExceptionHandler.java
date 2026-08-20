package com.csnotes.rag.answer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RagAnswerController.class)
public class RagAnswerExceptionHandler {
    @ExceptionHandler(RagAnswerValidationException.class)
    ProblemDetail invalidAnswerRequest(RagAnswerValidationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("RAG 답변 요청이 올바르지 않습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
