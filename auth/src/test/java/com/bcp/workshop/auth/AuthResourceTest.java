package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue());
    }

    @Test
    void logsInWithTrainerUser() {
        login("trainer", "Trainer123!")
                .statusCode(200)
                .body("user.role", is("TRAINER"));
    }

    @Test
    void rejectsIncorrectPasswordForExistingUser() {
        login("student", "incorrect-password")
                .statusCode(401)
                .body("$", not(hasKey("token")));
    }

    @Test
    void rejectsUnknownUser() {
        login("unknown-user", "Workshop123!")
                .statusCode(401)
                .body("$", not(hasKey("token")));
    }

    @Test
    void rejectsEmptyUsername() {
        login("", "Workshop123!")
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsEmptyPassword() {
        login("student", "")
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsSqlLikeUsernameWithArbitraryPassword() {
        login("' OR '1'='1", "arbitrary-password")
                .statusCode(401)
                .body("$", not(hasKey("token")));
    }

    @Test
    void rejectsSqlLikePasswordForExistingUser() {
        login("student", "' OR '1'='1")
                .statusCode(401)
                .body("$", not(hasKey("token")));
    }

    @Test
    void rejectsSqlLikeUsernameAndPassword() {
        login("' OR '1'='1", "' OR '1'='1")
                .statusCode(401)
                .body("$", not(hasKey("token")));
    }

    private ValidatableResponse login(String username, String password) {
        return given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then();
    }
}
