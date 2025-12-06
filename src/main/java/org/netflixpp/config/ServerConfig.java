package org.netflixpp.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerConfig {
    private static final Properties props = new Properties();

    public static void loadConfig() {
        try {
            // Default configuration
            props.setProperty("server.port", "8080");
            props.setProperty("nginx.port", "80");
            props.setProperty("mesh.port", "9001");
            props.setProperty("storage.path", "storage");
            props.setProperty("log.level", "INFO");
            props.setProperty("cors.allowed.origins", "*");
            props.setProperty("jwt.secret", "NetflixPP_Server_Secret_ChangeInProduction_2024");
            props.setProperty("chunk.size", "10485760"); // 10MB in bytes

            // Load from config file if exists
            File configFile = new File("server.properties");
            if (configFile.exists()) {
                props.load(Files.newInputStream(configFile.toPath()));
                System.out.println("📄 Loaded configuration from server.properties");
            }

            // Create required directories
            createDirectories();

        } catch (Exception e) {
            System.err.println("⚠️ Could not load configuration: " + e.getMessage());
        }
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(getStoragePath(), "movies"));
            Files.createDirectories(Paths.get(getStoragePath(), "chunks"));
            Files.createDirectories(Paths.get(getStoragePath(), "temp"));
            Files.createDirectories(Paths.get("logs"));
            Files.createDirectories(Paths.get("nginx"));
        } catch (Exception e) {
            System.err.println("⚠️ Could not create directories: " + e.getMessage());
        }
    }

    // Getters for configuration values
    public static int getServerPort() {
        return Integer.parseInt(props.getProperty("server.port"));
    }

    public static int getNginxPort() {
        return Integer.parseInt(props.getProperty("nginx.port"));
    }

    public static int getMeshPort() {
        return Integer.parseInt(props.getProperty("mesh.port"));
    }

    public static String getStoragePath() {
        return props.getProperty("storage.path");
    }

    public static String getLogLevel() {
        return props.getProperty("log.level");
    }

    public static String getCorsAllowedOrigins() {
        return props.getProperty("cors.allowed.origins");
    }

    public static String getJwtSecret() {
        return props.getProperty("jwt.secret");
    }

    public static int getChunkSize() {
        return Integer.parseInt(props.getProperty("chunk.size"));
    }

    public static Properties getProperties() {
        return new Properties(props);
    }

    public static void saveConfig() {
        try {
            props.store(Files.newOutputStream(Paths.get("server.properties")),
                    "Netflix++ Server Configuration");
            System.out.println("💾 Configuration saved to server.properties");
        } catch (Exception e) {
            System.err.println("❌ Could not save configuration: " + e.getMessage());
        }
    }
}