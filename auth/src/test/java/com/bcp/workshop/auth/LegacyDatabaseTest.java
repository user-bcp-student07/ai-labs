package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDatabaseTest {
    @Test
    void migratesPlainTextPasswordsOnlyOnce() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE legacy_users (id INTEGER PRIMARY KEY, username TEXT, password TEXT)");
            statement.executeUpdate("INSERT INTO legacy_users VALUES (1, 'legacy-user', 'legacy-password')");

            LegacyDatabase.migratePlainTextPasswords(connection);
            String migratedPassword = readPassword(connection);
            LegacyDatabase.migratePlainTextPasswords(connection);

            assertNotEquals("legacy-password", migratedPassword);
            assertTrue(PasswordHasher.matches("legacy-password", migratedPassword));
            assertEquals(migratedPassword, readPassword(connection));
        }
    }

    private String readPassword(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT password FROM legacy_users WHERE id = ?")) {
            statement.setLong(1, 1L);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString("password");
            }
        }
    }
}