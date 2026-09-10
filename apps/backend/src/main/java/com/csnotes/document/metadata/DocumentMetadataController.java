package com.csnotes.document.metadata;

import com.csnotes.auth.CsNotesOidcUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents/metadata")
@ConditionalOnProperty(name = {"cs-notes.security.enabled", "rag.persistence.enabled"}, havingValue = "true")
public class DocumentMetadataController {
    private final DocumentMetadataSyncService syncService;

    public DocumentMetadataController(DocumentMetadataSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.OK)
    public DocumentMetadataSyncResult synchronize(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CsNotesOidcUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return syncService.synchronize(principal.userId());
    }
}
