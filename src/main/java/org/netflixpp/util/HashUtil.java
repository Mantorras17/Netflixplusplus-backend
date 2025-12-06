package org.netflixpp.util;

import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;

public class HashUtil {

    public static String calculateFileHash(String filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            return calculateHash(is);
        }
    }

    public static String calculateHash(InputStream is) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);

        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    public static String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}