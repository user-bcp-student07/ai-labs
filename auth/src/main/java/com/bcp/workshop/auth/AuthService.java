package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Optional;

@ApplicationScoped
public class AuthService {
    private final LegacyDatabase database;
    private final JwtTokenService tokenService;

    @Inject
    public AuthService(LegacyDatabase database, JwtTokenService tokenService) {
        this.database = database;
        this.tokenService = tokenService;
    }

    public Optional<LoginResponse> authenticate(String username, String password) {
        UserRecord user = database.findByCredentials(username, password);
        if (user == null) {
            return Optional.empty();
        }

        LoginResponse.User responseUser = new LoginResponse.User(
                user.username(),
                StringEscapeUtils.escapeHtml4(user.displayName()),
                user.role()
        );
        return Optional.of(new LoginResponse(
                tokenService.issue(user.username(), user.role()),
                JwtTokenService.EXPIRATION_SECONDS,
                responseUser,
                "authenticated-with-legacy-flow"
        ));
    }
}