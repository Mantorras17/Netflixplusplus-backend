package org.netflixpp.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConfig {

    // MariaDB Configuration
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "netflixpp");
    private static final String DB_USER =
            System.getenv().getOrDefault("DB_USER", "admin");
    private static final String DB_PASS =
            System.getenv().getOrDefault("DB_PASS", "admin");

    private static final String MARIADB_URL =
            String.format("jdbc:mariadb://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);

    // Cassandra Configuration (opcional)
    private static final String CASSANDRA_HOST =
            System.getenv().getOrDefault("CASSANDRA_HOST", "localhost");
    private static final String CASSANDRA_PORT =
            System.getenv().getOrDefault("CASSANDRA_PORT", "9042");

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            System.out.println("✅ MariaDB driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MariaDB driver not found");
        }
    }

    public static Connection getMariaDB() {
        try {
            return DriverManager.getConnection(MARIADB_URL, DB_USER, DB_PASS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to MariaDB: " + e.getMessage(), e);
        }
    }

    public static String getCassandraHost() {
        return CASSANDRA_HOST + ":" + CASSANDRA_PORT;
    }
}