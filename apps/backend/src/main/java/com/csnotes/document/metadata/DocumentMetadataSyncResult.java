package com.csnotes.document.metadata;

public record DocumentMetadataSyncResult(
        int scanned,
        int created,
        int updated,
        int unchanged,
        int deleted,
        int ownershipConflicts
) {
}
