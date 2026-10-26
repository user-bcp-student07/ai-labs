package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void hashesWithUniqueSaltAndVerifiesPassword() {
        String password = "isolated-test-password";

        String firstHash = PasswordHasher.hash(password);
        String secondHash = PasswordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.matches(password, firstHash));
        assertFalse(PasswordHasher.matches("incorrect-password", firstHash));
    }

    @Test
    void rejectsMalformedHashes() {
        assertFalse(PasswordHasher.matches("password", "not-a-password-hash"));
        assertFalse(PasswordHasher.matches("password", "pbkdf2-sha256$999999999$bad$bad"));
        assertFalse(PasswordHasher.matches("password", "pbkdf2-sha256$600000$!$!"));
        assertFalse(PasswordHasher.matches(null, "not-a-password-hash"));
        assertFalse(PasswordHasher.matches("password", null));
    }
}