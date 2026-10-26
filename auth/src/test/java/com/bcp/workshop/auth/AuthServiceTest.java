package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    @Test
    void buildsResponseForAuthenticatedUser() {
        AuthService service = new AuthService(new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return new UserRecord(1L, username, "<Student>", "STUDENT");
            }
        });

        Optional<LoginResponse> response = service.authenticate("student", "password");

        assertTrue(response.isPresent());
        assertEquals("student", response.get().user().username());
        assertEquals("&lt;Student&gt;", response.get().user().displayName());
        assertEquals("STUDENT", response.get().user().role());
        assertEquals(LegacyTokenUtil.EXPIRATION_SECONDS, response.get().expiresIn());
        assertEquals("authenticated-with-legacy-flow", response.get().debug());
    }

    @Test
    void returnsEmptyWhenCredentialsAreInvalid() {
        AuthService service = new AuthService(new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return null;
            }
        });

        assertTrue(service.authenticate("unknown-user", "incorrect-password").isEmpty());
    }
}