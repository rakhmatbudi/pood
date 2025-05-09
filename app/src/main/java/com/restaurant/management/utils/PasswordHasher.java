package com.restaurant.management.utils;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {

    private static final String TAG = "PasswordHasher";
    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "SHA-256";

    /**
     * Hash a password with a randomly generated salt
     *
     * @param password The password to hash
     * @return The hashed password (format: salt:hash)
     */
    public static String hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash the password with the salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Convert to base64 strings
            String saltStr = Base64.getEncoder().encodeToString(salt);
            String hashedPasswordStr = Base64.getEncoder().encodeToString(hashedPassword);

            // Combine salt and hash
            String result = saltStr + ":" + hashedPasswordStr;
            Log.d(TAG, "Password hashed successfully: " + result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            // For testing, just return the plain password with a prefix
            return "PLAIN:" + password;
        }
    }

    /**
     * Check if a password matches a hash
     *
     * @param password The password to check
     * @param hashedPassword The hashed password (format: salt:hash)
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        try {
            // Handle plain text passwords for testing
            if (hashedPassword.startsWith("PLAIN:")) {
                String plainPassword = hashedPassword.substring(6);
                boolean matches = password.equals(plainPassword);
                Log.d(TAG, "Plain password check: " + matches);
                return matches;
            }

            // Split into salt and hash
            String[] parts = hashedPassword.split(":");
            if (parts.length != 2) {
                Log.e(TAG, "Invalid hash format: " + hashedPassword);
                return false;
            }

            // Decode from base64
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] hash = Base64.getDecoder().decode(parts[1]);

            // Hash the input password with the same salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] inputHash = md.digest(password.getBytes());

            // Compare the hashes
            if (inputHash.length != hash.length) {
                Log.e(TAG, "Hash length mismatch");
                return false;
            }

            boolean matches = true;
            for (int i = 0; i < hash.length; i++) {
                if (hash[i] != inputHash[i]) {
                    matches = false;
                    break;
                }
            }

            Log.d(TAG, "Password check result: " + matches);
            return matches;
        } catch (Exception e) {
            Log.e(TAG, "Error checking password", e);
            return false;
        }
    }
}