package com.csnotes.document;

public class DocumentReadException extends RuntimeException {

    public DocumentReadException(String message) {
        super(message);
    }

    public DocumentReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
