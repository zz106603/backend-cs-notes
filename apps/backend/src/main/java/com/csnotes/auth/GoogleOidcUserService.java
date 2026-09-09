package com.csnotes.auth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class GoogleOidcUserService extends OidcUserService {
    private final AppUserRepository appUserRepository;

    public GoogleOidcUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /** 검증된 Google ID 토큰의 sub를 기준으로 내부 사용자를 생성하거나 갱신한다. */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser googleUser = super.loadUser(userRequest);
        String subject = googleUser.getSubject();
        String email = googleUser.getEmail();
        String displayName = googleUser.getFullName();
        String pictureUrl = googleUser.getPicture();
        AppUser appUser = appUserRepository.upsertGoogleUser(
                subject,
                email,
                displayName == null || displayName.isBlank() ? email : displayName,
                pictureUrl
        );
        return new CsNotesOidcUser(appUser, googleUser);
    }
}
