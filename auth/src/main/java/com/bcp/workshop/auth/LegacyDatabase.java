package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LegacyDatabase {
    private static final String DB_URL = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(DB_URL);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
            }
            seedUserIfMissing("student", "Workshop123!", "Workshop Student", "STUDENT");
            seedUserIfMissing("trainer", "Trainer123!", "Workshop Trainer", "TRAINER");
            migratePlainTextPasswords();
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    void seedUserIfMissing(String username, String password, String displayName, String role) throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement lookup = connection.prepareStatement("SELECT 1 FROM legacy_users WHERE username = ?")) {
            lookup.setString(1, username);
            try (ResultSet result = lookup.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO legacy_users(username, password, display_name, role) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, username);
                insert.setString(2, PasswordHasher.hash(password));
                insert.setString(3, displayName);
                insert.setString(4, role);
                insert.executeUpdate();
            }
        }
    }

    private void migratePlainTextPasswords() throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            migratePlainTextPasswords(connection);
        }
    }

    static void migratePlainTextPasswords(Connection connection) throws Exception {
        List<StoredPassword> plainTextPasswords = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT id, password FROM legacy_users")) {
            while (result.next()) {
                String storedPassword = result.getString("password");
                if (!PasswordHasher.isEncodedHash(storedPassword)) {
                    plainTextPasswords.add(new StoredPassword(result.getLong("id"), storedPassword));
                }
            }
        }

        try (PreparedStatement update = connection.prepareStatement("UPDATE legacy_users SET password = ? WHERE id = ?")) {
            for (StoredPassword storedPassword : plainTextPasswords) {
                update.setString(1, PasswordHasher.hash(storedPassword.value()));
                update.setLong(2, storedPassword.id());
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    public UserRecord findByCredentials(String username, String password) {
        String sql = "SELECT id, username, password, display_name, role FROM legacy_users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && PasswordHasher.matches(password, result.getString("password"))) {
                    return new UserRecord(result.getLong("id"), result.getString("username"), result.getString("display_name"), result.getString("role"));
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return null;
    }

    private record StoredPassword(long id, String value) {}
}
