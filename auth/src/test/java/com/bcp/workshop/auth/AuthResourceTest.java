package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AuthResourceTest {
    @Inject
    LegacyDatabase database;

    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"))
                .body("token", notNullValue());
    }

    @Test
    void logsInWithWorkshopTrainer() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"trainer\",\"password\":\"Trainer123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("trainer"))
                .body("token", notNullValue());
    }

    @Test
    void reportsHealthyStatusWithoutAuthentication() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", hasSize(0));
    }

    @Test
    void seedsNewUserWithHashedPassword() throws Exception {
        String username = "auth-seed-" + UUID.randomUUID();
        database.seedUserIfMissing(username, "seed-password", "Seed User", "STUDENT");

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"seed-password\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .body("user.username", is(username))
                    .body("token", notNullValue());
        } finally {
            removeCharacterizationUser(System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db"), username);
        }
    }

    @Test
    void treatsQuotedUsernameAsData() throws Exception {
        String username = "auth'characterization-" + UUID.randomUUID();
        String databaseUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        insertCharacterizationUser(databaseUrl, username);

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"characterization-password\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .body("user.username", is(username))
                    .body("token", notNullValue());

            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"incorrect-password\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(401);
        } finally {
            removeCharacterizationUser(databaseUrl, username);
        }
    }

    @Test
    void rejectsNullRequest() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsBlankUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"   \",\"password\":\"password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsNullUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":null,\"password\":\"password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsBlankPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\" \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsNullPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":null}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsMalformedJson() {
        given()
                .contentType("application/json")
                .body("{bad")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body(is(""));
    }

    @Test
    void reportsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown-user\",\"password\":\"incorrect-password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user unknown-user or password"));
    }

    @Test
    void rejectsUnsupportedContentType() {
        given()
                .contentType("text/plain")
                .body("{}")
                .when().post("/auth/login")
                .then()
                .statusCode(415)
                .body(is(""));
    }

    @Test
    void rejectsUnsupportedHttpMethod() {
        given()
                .when().get("/auth/login")
                .then()
                .statusCode(405)
                .body(is(""));
    }

    @Test
    void returnsCurrentErrorDetailsForControlledFailure() {
        AuthResource resource = new AuthResource();
        resource.authService = new AuthService(new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                throw new IllegalStateException("controlled failure");
            }
        });
        LoginRequest request = new LoginRequest();
        request.username = "test-user";
        request.password = "test-password";

        Response response = resource.login(request);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(Map.of(
                "message", "controlled failure",
                "exception", IllegalStateException.class.getName()
        ), response.getEntity());
    }

    @Test
    void allowsDocumentedLocalCorsOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:4200"));
    }

    @Test
    void allowsOriginConfiguredForDeployment() {
        String configuredOrigin = System.getenv().getOrDefault("AUTH_CORS_ORIGINS", "http://localhost:4200");
        given()
                .header("Origin", configuredOrigin)
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", equalTo(configuredOrigin));
    }

    @Test
    void rejectsUnconfiguredCorsOrigin() {
        given()
                .header("Origin", "https://unconfigured.invalid")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    private void insertCharacterizationUser(String databaseUrl, String username) throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO legacy_users(username, password, display_name, role) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, PasswordHasher.hash("characterization-password"));
            statement.setString(3, "Characterization User");
            statement.setString(4, "STUDENT");
            statement.executeUpdate();
        }
    }

    private void removeCharacterizationUser(String databaseUrl, String username) throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM legacy_users WHERE username = ?")) {
            statement.setString(1, username);
            statement.executeUpdate();
        }
    }
}
