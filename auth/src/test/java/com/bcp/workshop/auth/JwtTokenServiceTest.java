package com.bcp.workshop.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {
    private static final String TEST_KEY = "test-only-signing-key-0123456789abcdef";
    private static final String OTHER_KEY = "another-test-key-0123456789abcdefgh";

    @Test
    void issuesTokenThatCanBeValidated() {
        JwtTokenService service = new JwtTokenService(TEST_KEY);

        String token = service.issue("student", "STUDENT");

        assertTrue(service.isValid(token));
    }

    @Test
    void issuedTokenCarriesSubjectRoleAndOneHourLifetime() {
        JwtTokenService service = new JwtTokenService(TEST_KEY);

        Claims claims = parse(service.issue("student", "STUDENT"), TEST_KEY);

        assertEquals("student", claims.getSubject());
        assertEquals("STUDENT", claims.get(JwtTokenService.ROLE_CLAIM, String.class));
        assertEquals(JwtTokenService.EXPIRATION_SECONDS,
                (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000);
    }

    @Test
    void rejectsTokenWithAlteredPayload() {
        JwtTokenService service = new JwtTokenService(TEST_KEY);
        String token = service.issue("student", "STUDENT");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"role\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertFalse(service.isValid(forged));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String foreignToken = new JwtTokenService(OTHER_KEY).issue("student", "STUDENT");

        assertFalse(new JwtTokenService(TEST_KEY).isValid(foreignToken));
    }

    @Test
    void rejectsMalformedToken() {
        assertFalse(new JwtTokenService(TEST_KEY).isValid("not-a-jwt"));
    }

    @Test
    void failsWhenSigningKeyIsBlank() {
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenService("").issue("student", "STUDENT"));
    }

    private static Claims parse(String token, String key) {
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
