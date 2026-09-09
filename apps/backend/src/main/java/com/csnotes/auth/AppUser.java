package com.csnotes.auth;

import java.util.UUID;

public record AppUser(
        UUID id,
        String provider,
        String providerSubject,
        String email,
        String displayName,
        String pictureUrl,
        String role
) {
}
