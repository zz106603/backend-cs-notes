package com.csnotes.document;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DocumentExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail handleNotFound(DocumentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DocumentConflictException.class)
    ProblemDetail handleConflict(DocumentConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({InvalidDocumentPathException.class, MethodArgumentNotValidException.class})
    ProblemDetail handleBadRequest(Exception exception) {
        String detail = exception instanceof MethodArgumentNotValidException
                ? "입력한 문서 정보를 확인해 주세요."
                : exception.getMessage();
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(DocumentReadException.class)
    ProblemDetail handleStorageError(DocumentReadException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
