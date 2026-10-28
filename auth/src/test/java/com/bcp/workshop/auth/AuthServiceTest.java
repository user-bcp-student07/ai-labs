package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private static final String TEST_KEY = "test-only-signing-key-0123456789abcdef";

    @Test
    void buildsResponseForAuthenticatedUser() {
        AuthService service = new AuthService(new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return new UserRecord(1L, username, "<Student>", "STUDENT");
            }
        }, new JwtTokenService(TEST_KEY));

        Optional<LoginResponse> response = service.authenticate("student", "password");

        assertTrue(response.isPresent());
        assertEquals("student", response.get().user().username());
        assertEquals("&lt;Student&gt;", response.get().user().displayName());
        assertEquals("STUDENT", response.get().user().role());
        assertEquals(JwtTokenService.EXPIRATION_SECONDS, response.get().expiresIn());
        assertEquals("authenticated-with-legacy-flow", response.get().debug());
    }

    @Test
    void returnsEmptyWhenCredentialsAreInvalid() {
        AuthService service = new AuthService(new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return null;
            }
        }, new JwtTokenService(TEST_KEY));

        assertTrue(service.authenticate("unknown-user", "incorrect-password").isEmpty());
    }
}