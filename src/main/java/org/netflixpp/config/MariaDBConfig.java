package org.netflixpp.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class MariaDBConfig {
    private static final String URL = "jdbc:mariadb://localhost:3306/netflixppdb";
    private static final String USER = "root";
    private static final String PASSWORD = "admin";

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB driver not found", e);
        }
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to MariaDB", e);
        }
    }
}
