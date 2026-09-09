package com.csnotes.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {
    @Test
    void 인증이_비활성화되면_로컬_사용을_허용한다() {
        AuthController controller = new AuthController(false);

        AuthController.AuthStatusResponse response = controller.me(null, null);

        assertThat(response.securityEnabled()).isFalse();
        assertThat(response.authenticated()).isTrue();
        assertThat(response.user()).isNull();
    }
}
