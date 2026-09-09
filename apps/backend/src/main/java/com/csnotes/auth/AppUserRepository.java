package com.csnotes.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@ConditionalOnProperty(name = {"cs-notes.security.enabled", "rag.persistence.enabled"}, havingValue = "true")
public class AppUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AppUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Google 계정을 내부 UUID 사용자로 변환하고, 재로그인 시 최신 프로필만 갱신한다. */
    public AppUser upsertGoogleUser(String subject, String email, String displayName, String pictureUrl) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO app_user (id, provider, provider_subject, email, display_name, picture_url)
                        VALUES (?, 'google', ?, ?, ?, ?)
                        ON CONFLICT (provider, provider_subject) DO UPDATE SET
                            email = EXCLUDED.email,
                            display_name = EXCLUDED.display_name,
                            picture_url = EXCLUDED.picture_url,
                            updated_at = CURRENT_TIMESTAMP
                        RETURNING id, provider, provider_subject, email, display_name, picture_url, role
                        """,
                (resultSet, rowNumber) -> new AppUser(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("provider"),
                        resultSet.getString("provider_subject"),
                        resultSet.getString("email"),
                        resultSet.getString("display_name"),
                        resultSet.getString("picture_url"),
                        resultSet.getString("role")
                ),
                UUID.randomUUID(), subject, email, displayName, pictureUrl);
    }
}
