package com.csnotes.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final boolean securityEnabled;

    public AuthController(@Value("${cs-notes.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    /** SPA가 최초 진입 시 로그인 상태와 이후 변경 요청에 필요한 CSRF 토큰을 함께 받는다. */
    @GetMapping("/me")
    public AuthStatusResponse me(Authentication authentication, CsrfToken csrfToken) {
        if (!securityEnabled) {
            return new AuthStatusResponse(false, true, null, null, null);
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof CsNotesOidcUser principal)) {
            return new AuthStatusResponse(true, false, null, csrfToken.getHeaderName(), csrfToken.getToken());
        }
        AppUser appUser = principal.appUser();
        AuthenticatedUserResponse user = new AuthenticatedUserResponse(
                appUser.id(), appUser.email(), appUser.displayName(), appUser.pictureUrl(), appUser.role());
        return new AuthStatusResponse(true, true, user, csrfToken.getHeaderName(), csrfToken.getToken());
    }

    public record AuthStatusResponse(
            boolean securityEnabled,
            boolean authenticated,
            AuthenticatedUserResponse user,
            String csrfHeaderName,
            String csrfToken
    ) {
    }

    public record AuthenticatedUserResponse(UUID id, String email, String displayName, String pictureUrl, String role) {
    }
}
