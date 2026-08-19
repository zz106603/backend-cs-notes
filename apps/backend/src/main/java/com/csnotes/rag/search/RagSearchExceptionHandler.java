package com.csnotes.rag.search;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RagSearchController.class)
public class RagSearchExceptionHandler {
    @ExceptionHandler(RagSearchValidationException.class)
    ProblemDetail invalidSearch(RagSearchValidationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("RAG 검색 요청이 올바르지 않습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
