package com.csnotes.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.Assert;

@Configuration
@ConditionalOnProperty(name = "cs-notes.security.enabled", havingValue = "true")
public class GoogleOidcSecurityConfiguration {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${cs-notes.security.google.client-id}") String clientId,
            @Value("${cs-notes.security.google.client-secret}") String clientSecret
    ) {
        Assert.hasText(clientId, "GOOGLE_OAUTH_CLIENT_ID 환경 변수가 필요합니다.");
        Assert.hasText(clientSecret, "GOOGLE_OAUTH_CLIENT_SECRET 환경 변수가 필요합니다.");
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    GoogleOidcUserService googleOidcUserService(AppUserRepository appUserRepository) {
        return new GoogleOidcUserService(appUserRepository);
    }

    @Bean
    SecurityFilterChain googleOidcSecurityFilterChain(
            HttpSecurity http,
            GoogleOidcUserService googleOidcUserService,
            @Value("${cs-notes.security.frontend-url}") String frontendUrl
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/me", "/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                        .successHandler((request, response, authentication) -> response.sendRedirect(frontendUrl))
                        .failureHandler((request, response, exception) -> response.sendRedirect(frontendUrl + "/?loginError=true")))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        (request, response, exception) -> response.sendError(401),
                        request -> request.getRequestURI().startsWith("/api/")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .build();
    }
}
