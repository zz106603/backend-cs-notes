package com.csnotes.document.metadata;

public class DocumentOwnershipConflictException extends RuntimeException {
    public DocumentOwnershipConflictException(String filePath) {
        super("다른 사용자가 소유한 문서 경로입니다: " + filePath);
    }
}
