package com.bcp.workshop.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_PREFIX = "pbkdf2-sha256";
    private static final int ITERATION_COUNT = 600_000;
    private static final int MAX_ITERATION_COUNT = 1_000_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int HASH_PART_COUNT = 4;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {}

    static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] derivedKey = derive(password, salt, ITERATION_COUNT);
        return HASH_PREFIX + "$" + ITERATION_COUNT + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivedKey);
    }

    static boolean matches(String password, String encodedHash) {
        if (password == null || encodedHash == null) {
            return false;
        }

        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != HASH_PART_COUNT || !HASH_PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATION_COUNT || iterations > MAX_ITERATION_COUNT) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedKey = Base64.getDecoder().decode(parts[3]);
            byte[] actualKey = derive(password, salt, iterations);
            return MessageDigest.isEqual(expectedKey, actualKey);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    static boolean isEncodedHash(String value) {
        return value != null && value.startsWith(HASH_PREFIX + "$");
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] passwordCharacters = password.toCharArray();
        PBEKeySpec keySpec = new PBEKeySpec(passwordCharacters, salt, iterations, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            return keyFactory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Password hashing is unavailable", error);
        } finally {
            keySpec.clearPassword();
            Arrays.fill(passwordCharacters, '\0');
        }
    }
}