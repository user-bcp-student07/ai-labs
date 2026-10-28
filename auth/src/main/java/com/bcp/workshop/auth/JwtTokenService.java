package com.bcp.workshop.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@ApplicationScoped
public class JwtTokenService {
    static final long EXPIRATION_SECONDS = 3600L;
    static final String ROLE_CLAIM = "role";

    @ConfigProperty(name = "auth.token.key")
    String signingKeyText;

    public JwtTokenService() {}

    JwtTokenService(String signingKeyText) {
        this.signingKeyText = signingKeyText;
    }

    public String issue(String username, String role) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(EXPIRATION_SECONDS)))
                .signWith(signingKey())
                .compact();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException error) {
            return false;
        }
    }

    private SecretKey signingKey() {
        if (signingKeyText == null || signingKeyText.isBlank()) {
            throw new IllegalStateException("Token signing key is not configured");
        }
        return Keys.hmacShaKeyFor(signingKeyText.getBytes(StandardCharsets.UTF_8));
    }
}
