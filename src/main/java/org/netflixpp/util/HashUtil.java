package org.netflixpp.util;

import java.security.MessageDigest;

public class HashUtil {
    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }

    public static String sha256(String data) {
        return sha256(data.getBytes());
    }
}